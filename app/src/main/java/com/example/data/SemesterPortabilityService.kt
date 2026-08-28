package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.Assessment
import com.example.model.Course
import com.example.model.Semester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class SemesterPortabilityService(private val context: Context) {
    private val dao = CollideDatabase.getDatabase(context).collideDao()

    /**
     * Exports full semester data to a single unified portable JSON string.
     */
    suspend fun exportSemesterToJson(semesterId: String): String = withContext(Dispatchers.IO) {
        val semester = dao.getSemesterById(semesterId)
            ?: throw IllegalArgumentException("Semester with ID $semesterId not found")

        val files = dao.getFilesListForSemester(semesterId)
        val courses = dao.getCoursesListForSemester(semesterId)
        val assessments = dao.getAssessmentsListForSemester(semesterId)
        val chatMessages = dao.getChatMessagesListForSemester(semesterId)

        val root = JSONObject().apply {
            put("collideVersion", "1.0.0")
            put("exportedAt", LocalDateTime.now().toString())
            put("app", "Collide — Semester Workload Engine")

            // Semester entity
            put("semester", JSONObject().apply {
                put("id", semester.id)
                put("name", semester.name)
                put("startDate", semester.startDate)
                put("weekCount", semester.weekCount)
                put("breaksJson", semester.breaksJson)
                put("capacityHoursPerWeek", semester.capacityHoursPerWeek)
                put("createdAt", semester.createdAt)
            })

            // Files
            put("syllabusFiles", JSONArray().apply {
                files.forEach { file ->
                    put(JSONObject().apply {
                        put("id", file.id)
                        put("fileName", file.fileName)
                        put("fileSizeBytes", file.fileSizeBytes)
                        put("pageCount", file.pageCount)
                        put("fileSha256", file.fileSha256)
                        put("status", file.status)
                        put("addedAt", file.addedAt)
                    })
                }
            })

            // Courses
            put("courses", JSONArray().apply {
                courses.forEach { course ->
                    put(JSONObject().apply {
                        put("id", course.id)
                        put("fileId", course.fileId)
                        put("code", course.code)
                        put("title", course.title)
                        put("instructor", course.instructor ?: "")
                        put("credits", course.credits ?: 0.0)
                        put("gradeBreakdownJson", course.gradeBreakdownJson)
                        put("latePolicy", course.latePolicy ?: "")
                        put("attendancePolicy", course.attendancePolicy ?: "")
                        put("resubmissionPolicy", course.resubmissionPolicy ?: "")
                        put("sourceFileName", course.sourceFileName)
                        put("sourcePage", course.sourcePage)
                        put("sourceQuote", course.sourceQuote)
                        put("confidence", course.confidence)
                    })
                }
            })

            // Assessments
            put("assessments", JSONArray().apply {
                assessments.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("courseId", a.courseId)
                        put("title", a.title)
                        put("type", a.type)
                        put("weightPercent", a.weightPercent)
                        put("dueDate", a.dueDate ?: "")
                        put("dueTime", a.dueTime ?: "")
                        put("dateBasis", a.dateBasis)
                        put("estimatedHours", a.estimatedHours)
                        put("hoursBasis", a.hoursBasis)
                        put("status", a.status)
                        put("sourceFileName", a.sourceFileName)
                        put("sourcePage", a.sourcePage)
                        put("sourceQuote", a.sourceQuote)
                        put("confidence", a.confidence)
                        put("isConfirmed", a.isConfirmed)
                    })
                }
            })

            // Chat History
            put("chatHistory", JSONArray().apply {
                chatMessages.forEach { msg ->
                    put(JSONObject().apply {
                        put("id", msg.id)
                        put("sender", msg.sender)
                        put("text", msg.text)
                        put("timestamp", msg.timestamp)
                    })
                }
            })
        }

        root.toString(2)
    }

    /**
     * Generates a standard RFC 5545 iCalendar (.ics) export of confirmed deadlines.
     */
    suspend fun exportConfirmedDeadlinesToIcs(semesterId: String): String = withContext(Dispatchers.IO) {
        val semester = dao.getSemesterById(semesterId)
            ?: throw IllegalArgumentException("Semester with ID $semesterId not found")

        val courses = dao.getCoursesListForSemester(semesterId).associateBy { it.id }
        val assessments = dao.getAssessmentsListForSemester(semesterId)
            .filter { it.isConfirmed && !it.dueDate.isNullOrBlank() }

        val dtstamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        val calName = "${semester.name} Deadlines (Collide)"

        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Collide//Semester Workload Engine//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")
        sb.appendLine("X-WR-CALNAME:$calName")
        sb.appendLine("X-WR-TIMEZONE:UTC")

        for (assessment in assessments) {
            val dueDateStr = assessment.dueDate ?: continue
            val course = courses[assessment.courseId]
            val courseCode = course?.code ?: "COURSE"
            val courseTitle = course?.title ?: ""

            // Parse date (YYYY-MM-DD)
            val dateClean = dueDateStr.replace("-", "")
            val uid = "collide-${assessment.id}@collide.app"

            val summary = "$courseCode: ${assessment.title} (${assessment.weightPercent.toInt()}%)"
            val descBuilder = StringBuilder().apply {
                append("Assessment: ${assessment.title}\\n")
                append("Course: $courseCode $courseTitle\\n")
                append("Type: ${assessment.type}\\n")
                append("Weight: ${assessment.weightPercent.toInt()}% of course grade\\n")
                append("Estimated workload: ${assessment.estimatedHours.toInt()} hours\\n")
                if (!assessment.hoursBasis.isNullOrBlank()) {
                    append("Workload basis: ${escapeIcsText(assessment.hoursBasis)}\\n")
                }
                if (!assessment.sourceFileName.isNullOrBlank()) {
                    append("Source: ${assessment.sourceFileName} (page ${assessment.sourcePage})\\n")
                }
                if (!assessment.sourceQuote.isNullOrBlank()) {
                    append("Syllabus quote: \"${escapeIcsText(assessment.sourceQuote)}\"\\n")
                }
            }

            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:$uid")
            sb.appendLine("DTSTAMP:$dtstamp")
            sb.appendLine("DTSTART;VALUE=DATE:$dateClean")
            // Next day for all-day end
            val nextDay = try {
                LocalDate.parse(dueDateStr).plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
            } catch (e: Exception) {
                dateClean
            }
            sb.appendLine("DTEND;VALUE=DATE:$nextDay")
            sb.appendLine("SUMMARY:${escapeIcsText(summary)}")
            sb.appendLine("DESCRIPTION:${descBuilder.toString()}")
            sb.appendLine("STATUS:CONFIRMED")
            sb.appendLine("TRANSP:OPAQUE")
            sb.appendLine("CATEGORIES:ACADEMIC,DEADLINE,COLLIDE")
            sb.appendLine("END:VEVENT")
        }

        sb.appendLine("END:VCALENDAR")
        sb.toString()
    }

    private fun escapeIcsText(input: String): String {
        return input.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    /**
     * Imports a full semester JSON string into the local Room database.
     * Generates a new ID if requested or restores exact data.
     */
    suspend fun importSemesterFromJson(
        jsonString: String,
        generateNewIds: Boolean = false
    ): Result<SemesterEntity> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("semester")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid JSON format: missing 'semester' object"))
            }

            val semObj = root.getJSONObject("semester")
            val originalSemId = semObj.getString("id")
            val newSemId = if (generateNewIds) "sem_${UUID.randomUUID().toString().take(8)}" else originalSemId

            val semesterEntity = SemesterEntity(
                id = newSemId,
                name = semObj.optString("name", "Imported Semester"),
                startDate = semObj.optString("startDate", LocalDate.now().toString()),
                weekCount = semObj.optInt("weekCount", 16),
                breaksJson = semObj.optString("breaksJson", "[]"),
                capacityHoursPerWeek = semObj.optDouble("capacityHoursPerWeek", 25.0),
                createdAt = semObj.optString("createdAt", LocalDate.now().toString())
            )

            dao.insertSemester(semesterEntity)

            // Map old file ID to new file ID
            val fileIdMap = mutableMapOf<String, String>()
            if (root.has("syllabusFiles")) {
                val filesArray = root.getJSONArray("syllabusFiles")
                val fileEntities = mutableListOf<SyllabusFileEntity>()
                for (i in 0 until filesArray.length()) {
                    val fObj = filesArray.getJSONObject(i)
                    val oldFid = fObj.getString("id")
                    val newFid = if (generateNewIds) "file_${UUID.randomUUID().toString().take(8)}" else oldFid
                    fileIdMap[oldFid] = newFid

                    fileEntities.add(
                        SyllabusFileEntity(
                            id = newFid,
                            semesterId = newSemId,
                            fileName = fObj.optString("fileName", "syllabus.pdf"),
                            fileSizeBytes = fObj.optLong("fileSizeBytes", 0L),
                            pageCount = fObj.optInt("pageCount", 1),
                            localFilePath = "",
                            fileSha256 = fObj.optString("fileSha256", ""),
                            status = fObj.optString("status", "EXTRACTED"),
                            errorMessage = null,
                            addedAt = fObj.optString("addedAt", System.currentTimeMillis().toString())
                        )
                    )
                }
                if (fileEntities.isNotEmpty()) {
                    dao.insertSyllabusFiles(fileEntities)
                }
            }

            // Courses
            val courseIdMap = mutableMapOf<String, String>()
            if (root.has("courses")) {
                val coursesArray = root.getJSONArray("courses")
                val courseEntities = mutableListOf<CourseEntity>()
                for (i in 0 until coursesArray.length()) {
                    val cObj = coursesArray.getJSONObject(i)
                    val oldCid = cObj.getString("id")
                    val newCid = if (generateNewIds) "c_${UUID.randomUUID().toString().take(8)}" else oldCid
                    courseIdMap[oldCid] = newCid

                    val oldFileId = cObj.optString("fileId", "")
                    val mappedFileId = fileIdMap[oldFileId] ?: oldFileId

                    courseEntities.add(
                        CourseEntity(
                            id = newCid,
                            semesterId = newSemId,
                            fileId = mappedFileId,
                            code = cObj.optString("code", "COURSE"),
                            title = cObj.optString("title", "Course Title"),
                            instructor = cObj.optString("instructor").takeIf { it.isNotBlank() },
                            credits = if (cObj.has("credits")) cObj.optDouble("credits") else null,
                            gradeBreakdownJson = cObj.optString("gradeBreakdownJson", "[]"),
                            latePolicy = cObj.optString("latePolicy").takeIf { it.isNotBlank() },
                            attendancePolicy = cObj.optString("attendancePolicy").takeIf { it.isNotBlank() },
                            resubmissionPolicy = cObj.optString("resubmissionPolicy").takeIf { it.isNotBlank() },
                            sourceFileName = cObj.optString("sourceFileName", ""),
                            sourcePage = cObj.optInt("sourcePage", 1),
                            sourceQuote = cObj.optString("sourceQuote", ""),
                            confidence = cObj.optDouble("confidence", 0.95)
                        )
                    )
                }
                if (courseEntities.isNotEmpty()) {
                    dao.insertCourses(courseEntities)
                }
            }

            // Assessments
            if (root.has("assessments")) {
                val assessmentsArray = root.getJSONArray("assessments")
                val assessmentEntities = mutableListOf<AssessmentEntity>()
                for (i in 0 until assessmentsArray.length()) {
                    val aObj = assessmentsArray.getJSONObject(i)
                    val oldAid = aObj.getString("id")
                    val newAid = if (generateNewIds) "a_${UUID.randomUUID().toString().take(8)}" else oldAid

                    val oldCourseId = aObj.getString("courseId")
                    val mappedCourseId = courseIdMap[oldCourseId] ?: oldCourseId

                    assessmentEntities.add(
                        AssessmentEntity(
                            id = newAid,
                            courseId = mappedCourseId,
                            semesterId = newSemId,
                            title = aObj.optString("title", "Assessment"),
                            type = aObj.optString("type", "Assignment"),
                            weightPercent = aObj.optDouble("weightPercent", 10.0),
                            dueDate = aObj.optString("dueDate").takeIf { it.isNotBlank() },
                            dueTime = aObj.optString("dueTime").takeIf { it.isNotBlank() },
                            dateBasis = aObj.optString("dateBasis", "EXACT_DATE"),
                            estimatedHours = aObj.optDouble("estimatedHours", 4.0),
                            hoursBasis = aObj.optString("hoursBasis", ""),
                            status = aObj.optString("status", "CONFIRMED"),
                            sourceFileName = aObj.optString("sourceFileName", ""),
                            sourcePage = aObj.optInt("sourcePage", 1),
                            sourceQuote = aObj.optString("sourceQuote", ""),
                            confidence = aObj.optDouble("confidence", 0.95),
                            isConfirmed = aObj.optBoolean("isConfirmed", true)
                        )
                    )
                }
                if (assessmentEntities.isNotEmpty()) {
                    dao.insertAssessments(assessmentEntities)
                }
            }

            // Chat History
            if (root.has("chatHistory")) {
                val chatArray = root.getJSONArray("chatHistory")
                val chatEntities = mutableListOf<ChatMessageEntity>()
                for (i in 0 until chatArray.length()) {
                    val msgObj = chatArray.getJSONObject(i)
                    chatEntities.add(
                        ChatMessageEntity(
                            id = if (generateNewIds) UUID.randomUUID().toString() else msgObj.optString("id", UUID.randomUUID().toString()),
                            semesterId = newSemId,
                            sender = msgObj.optString("sender", "USER"),
                            text = msgObj.optString("text", ""),
                            timestamp = msgObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (chatEntities.isNotEmpty()) {
                    dao.insertChatMessages(chatEntities)
                }
            }

            Result.success(semesterEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Copies text to the Android system clipboard.
     */
    fun copyToClipboard(label: String, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, content)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Shares a text payload or exports as a file via Android Intent.ACTION_SEND.
     */
    fun shareContent(title: String, content: String, mimeType: String = "text/plain") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_TITLE, title)
            type = mimeType
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(shareIntent)
    }
}
