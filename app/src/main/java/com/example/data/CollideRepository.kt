package com.example.data

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.model.Assessment
import com.example.model.AssessmentStatus
import com.example.model.AssessmentType
import com.example.model.AssessmentTypeConfig
import com.example.model.Course
import com.example.model.CoursePolicies
import com.example.model.DEFAULT_TYPE_CONFIGS
import com.example.model.DateBasis
import com.example.model.GradeBreakdownItem
import com.example.model.HoursBasis
import com.example.model.LoadModelEngine
import com.example.model.Semester
import com.example.model.SemesterBreak
import com.example.model.SemesterLoadResult
import com.example.model.SourceRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class ExtractionProgress(
    val currentFileIndex: Int,
    val totalFiles: Int,
    val currentFileName: String,
    val statusText: String,
    val isRunning: Boolean = false
)

class CollideRepository(private val context: Context) {
    private val dao = CollideDatabase.getDatabase(context).collideDao()
    private val extractorService = GeminiExtractorService(context)
    val portabilityService = SemesterPortabilityService(context)

    fun getAllSemesters(): Flow<List<SemesterEntity>> = dao.getAllSemesters()

    suspend fun getSemesterById(semesterId: String): SemesterEntity? = dao.getSemesterById(semesterId)

    fun getFilesForSemester(semesterId: String): Flow<List<SyllabusFileEntity>> =
        dao.getFilesForSemester(semesterId)

    fun getCoursesForSemester(semesterId: String): Flow<List<Course>> =
        dao.getCoursesForSemester(semesterId).map { entities ->
            entities.map { mapCourseEntityToModel(it) }
        }

    fun getAssessmentsForSemester(semesterId: String): Flow<List<Assessment>> =
        dao.getAssessmentsForSemester(semesterId).map { entities ->
            entities.map { mapAssessmentEntityToModel(it) }
        }

    fun getChatMessagesForSemester(semesterId: String): Flow<List<ChatMessageEntity>> =
        dao.getChatMessagesForSemester(semesterId)

    suspend fun saveChatMessage(semesterId: String, sender: String, text: String) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                semesterId = semesterId,
                sender = sender,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearChatForSemester(semesterId: String) = withContext(Dispatchers.IO) {
        dao.clearChatForSemester(semesterId)
    }

    suspend fun createSemester(
        name: String,
        startDateIso: String,
        weekCount: Int,
        breaks: List<SemesterBreak>,
        capacityHoursPerWeek: Double
    ): SemesterEntity = withContext(Dispatchers.IO) {
        val pinnedStartDate = pinToMonday(startDateIso)
        val breaksJson = JSONArray().apply {
            breaks.forEach { b ->
                put(JSONObject().apply {
                    put("label", b.label)
                    put("start", b.start)
                    put("end", b.end)
                })
            }
        }.toString()

        val entity = SemesterEntity(
            id = "sem_${UUID.randomUUID().toString().take(8)}",
            name = name.trim().ifEmpty { "Semester" },
            startDate = pinnedStartDate,
            weekCount = weekCount.coerceIn(4, 24),
            breaksJson = breaksJson,
            capacityHoursPerWeek = capacityHoursPerWeek.coerceAtLeast(1.0),
            createdAt = LocalDate.now().toString()
        )
        dao.insertSemester(entity)
        entity
    }

    /**
     * Ensures any given ISO date is snapped/pinned to the preceding or exact Monday.
     */
    fun pinToMonday(dateStr: String): String {
        return try {
            val parsed = LocalDate.parse(dateStr.trim())
            if (parsed.dayOfWeek == DayOfWeek.MONDAY) {
                parsed.toString()
            } else {
                parsed.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)).toString()
            }
        } catch (e: Exception) {
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        }
    }

    /**
     * Ingests a user-selected URI or file.
     * Rejects non-PDF with the explicit Design Law error message:
     * "Collide reads PDFs. Export your syllabus as PDF and drop it again."
     */
    suspend fun ingestDocument(
        semesterId: String,
        uri: Uri
    ): Result<SyllabusFileEntity> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            var fileName = "syllabus_${System.currentTimeMillis()}.pdf"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    val resolvedName = cursor.getString(nameIndex)
                    if (!resolvedName.isNullOrBlank()) {
                        fileName = resolvedName
                    }
                }
            }

            val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                    (mimeType != null && mimeType.contains("pdf", ignoreCase = true))

            if (!isPdf) {
                return@withContext Result.failure(
                    IllegalArgumentException("Collide reads PDFs. Export your syllabus as PDF and drop it again.")
                )
            }

            // Copy content to internal app storage
            val syllabusDir = File(context.filesDir, "syllabi").apply { mkdirs() }
            val localFile = File(syllabusDir, "${UUID.randomUUID()}_$fileName")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(
                IllegalStateException("Could not read uploaded PDF file stream.")
            )

            // Determine page count & sha256, verifying PDF can be opened
            val pageCountResult = tryGetPdfPageCount(localFile)
            if (pageCountResult.isFailure) {
                val errorMsg = pageCountResult.exceptionOrNull()?.message 
                    ?: "That PDF could not be opened. Check the file and drop it again."
                localFile.delete()
                return@withContext Result.failure(IllegalArgumentException(errorMsg))
            }
            val pageCount = pageCountResult.getOrDefault(1)
            val sha256 = extractorService.calculateFileSha256(localFile)

            val fileEntity = SyllabusFileEntity(
                id = "file_${UUID.randomUUID().toString().take(8)}",
                semesterId = semesterId,
                fileName = fileName,
                fileSizeBytes = localFile.length(),
                pageCount = pageCount,
                localFilePath = localFile.absolutePath,
                fileSha256 = sha256,
                status = "Ready for extraction",
                errorMessage = null,
                addedAt = System.currentTimeMillis().toString()
            )

            dao.insertSyllabusFile(fileEntity)
            Result.success(fileEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads 6 sample syllabus PDFs for fast evaluation and inspection.
     */
    suspend fun loadSampleSyllabi(semesterId: String): List<SyllabusFileEntity> = withContext(Dispatchers.IO) {
        val sampleConfigs = listOf(
            SamplePdfInfo("PHYS 2041 Classical Mechanics Syllabus.pdf", 5),
            SamplePdfInfo("CS 3110 Functional Programming Course Guide.pdf", 8),
            SamplePdfInfo("MATH 2940 Linear Algebra Syllabus.pdf", 4),
            SamplePdfInfo("CHEM 2080 General Chemistry II Guide.pdf", 6),
            SamplePdfInfo("ENGL 2800 Creative Writing Syllabus.pdf", 3),
            SamplePdfInfo("ECON 1110 Introductory Microeconomics.pdf", 7)
        )

        val syllabusDir = File(context.filesDir, "syllabi").apply { mkdirs() }
        val entities = mutableListOf<SyllabusFileEntity>()

        for (sample in sampleConfigs) {
            val localFile = File(syllabusDir, "${UUID.randomUUID()}_${sample.name}")
            generateSamplePdfContent(localFile, sample.name, sample.pages)
            val sha256 = extractorService.calculateFileSha256(localFile)

            val entity = SyllabusFileEntity(
                id = "file_${UUID.randomUUID().toString().take(8)}",
                semesterId = semesterId,
                fileName = sample.name,
                fileSizeBytes = localFile.length(),
                pageCount = sample.pages,
                localFilePath = localFile.absolutePath,
                fileSha256 = sha256,
                status = "Ready for extraction",
                errorMessage = null,
                addedAt = System.currentTimeMillis().toString()
            )
            entities.add(entity)
        }

        dao.insertSyllabusFiles(entities)
        entities
    }

    /**
     * Serial batch extraction for all files in a semester.
     * Checks SHA-256 cache first so reopening never re-extracts or wastes tokens.
     */
    suspend fun extractAllFiles(
        semesterId: String,
        onProgress: (ExtractionProgress) -> Unit
    ): List<ExtractedSyllabusResult> = withContext(Dispatchers.IO) {
        val semester = dao.getSemesterById(semesterId) ?: return@withContext emptyList()
        val files = dao.getFilesListForSemester(semesterId)
        val breaks = parseBreaks(semester.breaksJson)

        val results = mutableListOf<ExtractedSyllabusResult>()
        val total = files.size

        files.forEachIndexed { index, fileEntity ->
            onProgress(
                ExtractionProgress(
                    currentFileIndex = index + 1,
                    totalFiles = total,
                    currentFileName = fileEntity.fileName,
                    statusText = "Extracting ${fileEntity.fileName}...",
                    isRunning = true
                )
            )

            dao.updateFileStatus(fileEntity.id, "Extracting")

            try {
                val file = File(fileEntity.localFilePath)
                val sha256 = if (fileEntity.fileSha256.isNotBlank()) {
                    fileEntity.fileSha256
                } else {
                    extractorService.calculateFileSha256(file)
                }

                // Check cache first
                val cached = dao.getExtractionCache(sha256)
                val extractionResult = if (cached != null) {
                    extractorService.parseExtractionResponse(
                        jsonString = cached.resultJson,
                        semesterId = semesterId,
                        defaultFileName = fileEntity.fileName,
                        semesterStartDate = semester.startDate
                    )
                } else {
                    val extracted = extractorService.extractSyllabus(
                        file = file,
                        fileName = fileEntity.fileName,
                        semesterId = semesterId,
                        semesterStartDate = semester.startDate,
                        weekCount = semester.weekCount,
                        breaks = breaks
                    )
                    // Save to cache
                    dao.insertExtractionCache(
                        ExtractionCacheEntity(
                            fileSha256 = sha256,
                            resultJson = extracted.rawJson,
                            extractedAt = System.currentTimeMillis().toString()
                        )
                    )
                    extracted
                }

                // Delete old course/assessments from this file if re-extracted
                dao.deleteCoursesForFile(fileEntity.id)

                // Save extracted course
                val courseEntity = CourseEntity(
                    id = extractionResult.course.id,
                    semesterId = semesterId,
                    fileId = fileEntity.id,
                    code = extractionResult.course.code,
                    title = extractionResult.course.title,
                    instructor = extractionResult.course.instructor,
                    credits = extractionResult.course.credits,
                    gradeBreakdownJson = JSONArray().apply {
                        extractionResult.course.gradeBreakdown.forEach { gb ->
                            put(JSONObject().apply {
                                put("label", gb.label)
                                put("weightPercent", gb.weightPercent)
                            })
                        }
                    }.toString(),
                    latePolicy = extractionResult.course.policies.late,
                    attendancePolicy = extractionResult.course.policies.attendance,
                    resubmissionPolicy = extractionResult.course.policies.resubmission,
                    sourceFileName = extractionResult.course.source.fileName,
                    sourcePage = extractionResult.course.source.page,
                    sourceQuote = extractionResult.course.source.quote,
                    confidence = extractionResult.course.confidence
                )
                dao.insertCourses(listOf(courseEntity))

                // Save extracted assessments
                val assessmentEntities = extractionResult.assessments.map { a ->
                    AssessmentEntity(
                        id = a.id,
                        courseId = extractionResult.course.id,
                        semesterId = semesterId,
                        title = a.title,
                        type = a.type.displayName,
                        weightPercent = a.weightPercent,
                        dueDate = a.dueDate,
                        dueTime = a.dueTime,
                        dateBasis = a.dateBasis.key,
                        estimatedHours = a.estimatedHours,
                        hoursBasis = a.hoursBasis.key,
                        status = a.status.key,
                        sourceFileName = a.source.fileName,
                        sourcePage = a.source.page,
                        sourceQuote = a.source.quote,
                        confidence = a.confidence,
                        isConfirmed = a.isConfirmed
                    )
                }
                dao.insertAssessments(assessmentEntities)

                dao.updateFileStatus(fileEntity.id, "Extracted")
                results.add(extractionResult)

            } catch (e: Exception) {
                dao.updateFileStatus(fileEntity.id, "Error", e.message)
            }
        }

        onProgress(
            ExtractionProgress(
                currentFileIndex = total,
                totalFiles = total,
                currentFileName = "",
                statusText = "Extraction complete",
                isRunning = false
            )
        )

        results
    }

    suspend fun deleteSyllabusFile(fileId: String) = withContext(Dispatchers.IO) {
        dao.deleteCoursesForFile(fileId)
        dao.deleteSyllabusFile(fileId)
    }

    private fun parseBreaks(breaksJson: String): List<SemesterBreak> {
        return try {
            val array = JSONArray(breaksJson)
            val list = mutableListOf<SemesterBreak>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SemesterBreak(
                        label = obj.optString("label", "Break"),
                        start = obj.optString("start"),
                        end = obj.optString("end")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapCourseEntityToModel(entity: CourseEntity): Course {
        val gradeBreakdown = mutableListOf<GradeBreakdownItem>()
        try {
            val array = JSONArray(entity.gradeBreakdownJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                gradeBreakdown.add(
                    GradeBreakdownItem(
                        label = obj.optString("label"),
                        weightPercent = obj.optDouble("weightPercent")
                    )
                )
            }
        } catch (e: Exception) {
            // ignore
        }

        return Course(
            id = entity.id,
            semesterId = entity.semesterId,
            code = entity.code,
            title = entity.title,
            instructor = entity.instructor,
            credits = entity.credits,
            gradeBreakdown = gradeBreakdown,
            policies = CoursePolicies(
                late = entity.latePolicy,
                attendance = entity.attendancePolicy,
                resubmission = entity.resubmissionPolicy
            ),
            source = SourceRef(
                fileName = entity.sourceFileName,
                page = entity.sourcePage,
                quote = entity.sourceQuote
            ),
            confidence = entity.confidence
        )
    }

    private fun mapAssessmentEntityToModel(entity: AssessmentEntity): Assessment {
        val type = AssessmentType.values().find {
            it.displayName.equals(entity.type, ignoreCase = true) || it.name.equals(entity.type, ignoreCase = true)
        } ?: AssessmentType.OTHER

        val dateBasis = when (entity.dateBasis) {
            DateBasis.DERIVED_FROM_WEEK.key -> DateBasis.DERIVED_FROM_WEEK
            DateBasis.UNKNOWN.key -> DateBasis.UNKNOWN
            DateBasis.INFERRED.key -> DateBasis.INFERRED
            else -> DateBasis.STATED
        }

        val hoursBasis = if (entity.hoursBasis == HoursBasis.USER.key) HoursBasis.USER else HoursBasis.DEFAULT
        val status = when (entity.status) {
            AssessmentStatus.DOING.key -> AssessmentStatus.DOING
            AssessmentStatus.DONE.key -> AssessmentStatus.DONE
            else -> AssessmentStatus.TODO
        }

        return Assessment(
            id = entity.id,
            courseId = entity.courseId,
            title = entity.title,
            type = type,
            weightPercent = entity.weightPercent,
            dueDate = entity.dueDate,
            dueTime = entity.dueTime,
            dateBasis = dateBasis,
            estimatedHours = entity.estimatedHours,
            hoursBasis = hoursBasis,
            status = status,
            source = SourceRef(
                fileName = entity.sourceFileName,
                page = entity.sourcePage,
                quote = entity.sourceQuote
            ),
            confidence = entity.confidence,
            isConfirmed = entity.isConfirmed
        )
    }

    suspend fun updateSemesterCapacity(semesterId: String, capacity: Double) = withContext(Dispatchers.IO) {
        dao.updateSemesterCapacity(semesterId, capacity.coerceAtLeast(1.0))
    }

    private val prefs = context.getSharedPreferences("collide_load_settings", Context.MODE_PRIVATE)

    fun getLoadModelSettings(): Map<AssessmentType, AssessmentTypeConfig> {
        val result = mutableMapOf<AssessmentType, AssessmentTypeConfig>()
        AssessmentType.values().forEach { type ->
            val default = DEFAULT_TYPE_CONFIGS[type] ?: AssessmentTypeConfig(type, 6.0, 7)
            val baseHours = prefs.getFloat("base_hours_${type.name}", default.baseHours.toFloat()).toDouble()
            val windowDays = prefs.getInt("window_days_${type.name}", default.windowDays)
            result[type] = AssessmentTypeConfig(type, baseHours, windowDays)
        }
        return result
    }

    fun saveLoadModelSettings(configs: Map<AssessmentType, AssessmentTypeConfig>) {
        val editor = prefs.edit()
        configs.forEach { (type, config) ->
            editor.putFloat("base_hours_${type.name}", config.baseHours.toFloat())
            editor.putInt("window_days_${type.name}", config.windowDays)
        }
        editor.apply()
    }

    fun resetLoadModelSettings() {
        prefs.edit().clear().apply()
    }

    fun computeSemesterLoad(
        semester: SemesterEntity,
        courses: List<Course>,
        assessments: List<Assessment>,
        customConfigs: Map<AssessmentType, AssessmentTypeConfig> = getLoadModelSettings()
    ): SemesterLoadResult {
        return LoadModelEngine.computeSemesterLoad(
            semesterId = semester.id,
            startDateIso = semester.startDate,
            weekCount = semester.weekCount,
            breaks = parseBreaks(semester.breaksJson),
            capacityHoursPerWeek = semester.capacityHoursPerWeek,
            courses = courses,
            assessments = assessments,
            customConfigs = customConfigs
        )
    }

    suspend fun confirmAssessment(assessmentId: String) = withContext(Dispatchers.IO) {
        dao.confirmAssessment(assessmentId)
    }

    suspend fun bulkConfirmCourse(courseId: String) = withContext(Dispatchers.IO) {
        dao.bulkConfirmCourse(courseId)
    }

    suspend fun updateAssessment(assessment: Assessment, semesterId: String) = withContext(Dispatchers.IO) {
        val entity = AssessmentEntity(
            id = assessment.id,
            courseId = assessment.courseId,
            semesterId = semesterId,
            title = assessment.title,
            type = assessment.type.displayName,
            weightPercent = assessment.weightPercent,
            dueDate = assessment.dueDate,
            dueTime = assessment.dueTime,
            dateBasis = assessment.dateBasis.key,
            estimatedHours = assessment.estimatedHours,
            hoursBasis = assessment.hoursBasis.key,
            status = assessment.status.key,
            sourceFileName = assessment.source.fileName,
            sourcePage = assessment.source.page,
            sourceQuote = assessment.source.quote,
            confidence = assessment.confidence,
            isConfirmed = assessment.isConfirmed
        )
        dao.insertAssessment(entity)
    }

    suspend fun deleteAssessment(assessmentId: String) = withContext(Dispatchers.IO) {
        dao.deleteAssessment(assessmentId)
    }

    /**
     * Renders a specific PDF page to an Android Bitmap using native PdfRenderer.
     */
    suspend fun renderPdfPageBitmap(
        fileName: String,
        pageNumber: Int,
        targetWidth: Int = 800,
        targetHeight: Int = 1100
    ): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        try {
            val syllabusDir = File(context.filesDir, "syllabi")
            val targetFile = syllabusDir.listFiles()?.firstOrNull {
                it.name.endsWith(fileName, ignoreCase = true) || it.name.contains(fileName, ignoreCase = true)
            } ?: return@withContext null

            ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val renderer = PdfRenderer(pfd)
                val zeroIndexPage = (pageNumber - 1).coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
                val page = renderer.openPage(zeroIndexPage)

                val bitmap = android.graphics.Bitmap.createBitmap(
                    targetWidth,
                    targetHeight,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                bitmap
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun tryGetPdfPageCount(file: File): Result<Int> {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                renderer.close()
                Result.success(count)
            }
        } catch (e: SecurityException) {
            Result.failure(IllegalArgumentException("That PDF is password-protected. Remove the password and drop it again."))
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("password", ignoreCase = true) || msg.contains("encrypt", ignoreCase = true)) {
                Result.failure(IllegalArgumentException("That PDF is password-protected. Remove the password and drop it again."))
            } else if (file.length() == 0L) {
                Result.failure(IllegalArgumentException("That PDF file is empty (0 bytes). Check the file and drop it again."))
            } else {
                Result.failure(IllegalArgumentException("That PDF could not be parsed. Confirm it is a valid PDF document and drop it again."))
            }
        }
    }

    private fun getPdfPageCount(file: File): Int {
        return tryGetPdfPageCount(file).getOrDefault(1)
    }

    private fun generateSamplePdfContent(file: File, title: String, pages: Int) {
        try {
            val document = PdfDocument()
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 14f
                isAntiAlias = true
            }
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val subtitlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 12f
                isAntiAlias = true
            }
            val boxPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(40, 255, 220, 100) // subtle highlight
                style = android.graphics.Paint.Style.FILL
            }

            for (i in 1..pages) {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, i).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(android.graphics.Color.WHITE)

                // Header
                val cleanTitle = title.replace(".pdf", "")
                canvas.drawText(cleanTitle, 40f, 50f, titlePaint)
                canvas.drawText("Page $i of $pages  ·  Official University Syllabus Course Guide", 40f, 75f, subtitlePaint)
                canvas.drawLine(40f, 85f, 555f, 85f, subtitlePaint)

                // Draw structured content
                if (i == 1) {
                    canvas.drawText("1. Course Description & Learning Objectives", 40f, 120f, titlePaint)
                    canvas.drawText("This course provides rigorous study in foundational principles and methodology.", 40f, 145f, textPaint)
                    canvas.drawText("Prerequisites, attendance policies, and instructor details are listed below.", 40f, 165f, textPaint)

                    canvas.drawText("2. Grading Breakdown & Components", 40f, 210f, titlePaint)
                    canvas.drawRect(35f, 225f, 555f, 315f, boxPaint)
                    canvas.drawText("Graded assignments and examinations contribute to the final course evaluation.", 40f, 245f, textPaint)
                    canvas.drawText("Assessment weights: Problem Sets (25%), Midterms (25%), Labs/Essays (20%), Final (30%).", 40f, 270f, textPaint)
                    canvas.drawText("Late submission policy: 10% penalty per day up to 3 days maximum.", 40f, 295f, textPaint)
                } else if (i == 2 || i == 3) {
                    canvas.drawText("3. Weekly Calendar & Deadlines", 40f, 120f, titlePaint)
                    canvas.drawRect(35f, 135f, 555f, 280f, boxPaint)
                    canvas.drawText("• Week 3: Problem Set 1 / Essay 1 due Monday 5:00 PM.", 40f, 160f, textPaint)
                    canvas.drawText("• Week 5: Problem Set 2 due Monday before 5:00 PM.", 40f, 190f, textPaint)
                    canvas.drawText("• Week 7: Midterm Examination on Thursday in Auditorium 7:30 PM.", 40f, 220f, textPaint)
                    canvas.drawText("• Week 9: Laboratory Report 1 due Friday 11:59 PM.", 40f, 250f, textPaint)
                } else {
                    canvas.drawText("4. University Policies & Examinations", 40f, 120f, titlePaint)
                    canvas.drawText("Students are expected to adhere strictly to the university code of academic conduct.", 40f, 150f, textPaint)
                    canvas.drawText("Final Comprehensive Examination will take place during finals week.", 40f, 180f, textPaint)
                }

                document.finishPage(page)
            }
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
        } catch (e: Throwable) {
            // Write fallback minimal PDF byte stream
            FileOutputStream(file).use { out ->
                val pdfContent = "%PDF-1.4\n%âãÏÓ\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count $pages >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\nendobj\nxref\n0 4\n0000000000 65535 f \n0000000015 00000 n \n0000000068 00000 n \n0000000133 00000 n \ntrailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n211\n%%EOF\n"
                out.write(pdfContent.toByteArray(Charsets.ISO_8859_1))
            }
        }
    }

    data class SamplePdfInfo(val name: String, val pages: Int)
}
