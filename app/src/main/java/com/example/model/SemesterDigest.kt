package com.example.model

import com.example.data.SemesterEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

data class SemesterDigestContext(
    val digestJson: String,
    val onDemandSyllabusText: String?,
    val whatIfRecomputeSummary: String?
)

object SemesterDigestBuilder {

    /**
     * Constructs a compact, complete, grounded semester digest JSON.
     */
    fun buildDigest(
        semester: SemesterEntity,
        courses: List<Course>,
        assessments: List<Assessment>,
        loadResult: SemesterLoadResult?
    ): String {
        val root = JSONObject()

        // 1. Semester info
        val startDate = try {
            LocalDate.parse(semester.startDate)
        } catch (e: Exception) {
            LocalDate.now()
        }
        val today = LocalDate.now()
        val currentWeek = if (today.isBefore(startDate)) {
            1
        } else {
            val weeksPassed = ChronoUnit.WEEKS.between(startDate, today).toInt() + 1
            weeksPassed.coerceIn(1, semester.weekCount)
        }

        val semesterObj = JSONObject().apply {
            put("name", semester.name)
            put("week", currentWeek)
            put("of", semester.weekCount)
            put("capacity", semester.capacityHoursPerWeek.toInt())
            put("startDate", semester.startDate)
        }
        root.put("semester", semesterObj)

        // Course lookup
        val courseMap = courses.associateBy { it.id }

        // 2. Courses
        val coursesArr = JSONArray()
        courses.forEach { c ->
            val cObj = JSONObject().apply {
                put("code", c.code)
                put("title", c.title)
                c.credits?.let { put("credits", it) }
                c.instructor?.let { put("instructor", it) }

                val breakdownArr = JSONArray()
                c.gradeBreakdown.forEach { item ->
                    breakdownArr.put(JSONObject().apply {
                        put("category", item.label)
                        put("weight", item.weightPercent)
                    })
                }
                put("breakdown", breakdownArr)

                val policiesObj = JSONObject()
                c.policies.late?.let { policiesObj.put("late", it) }
                c.policies.attendance?.let { policiesObj.put("attendance", it) }
                c.policies.resubmission?.let { policiesObj.put("resubmissions", it) }
                put("policies", policiesObj)
                put("source", "${c.source.fileName}, page ${c.source.page}")
            }
            coursesArr.put(cObj)
        }
        root.put("courses", coursesArr)

        // 3. Assessments
        val assessmentsArr = JSONArray()
        assessments.forEach { a ->
            val course = courseMap[a.courseId]
            val aObj = JSONObject().apply {
                put("id", a.id)
                put("courseCode", course?.code ?: "COURSE")
                put("title", a.title)
                put("type", a.type.displayName)
                put("weight", a.weightPercent)
                a.dueDate?.let { put("due", it) }
                put("dateBasis", a.dateBasis.key)
                put("hours", a.estimatedHours)
                put("status", a.status.name.lowercase(Locale.ROOT))
                put("confirmed", a.isConfirmed)
                put("source", "${a.source.fileName}, page ${a.source.page}")
                if (a.source.quote.isNotBlank()) {
                    put("quote", a.source.quote)
                }
            }
            assessmentsArr.put(aObj)
        }
        root.put("assessments", assessmentsArr)

        // 4. Load weeks
        val loadArr = JSONArray()
        val collisionsArr = JSONArray()

        loadResult?.weeks?.forEach { w ->
            val wObj = JSONObject().apply {
                put("week", w.weekNumber)
                put("hours", (Math.round(w.totalHours * 10.0) / 10.0))
                put("pressure", (Math.round(w.pressure * 100.0) / 100.0))
                put("band", w.band.label)
            }
            loadArr.put(wObj)

            if (w.band == PressureBand.COLLISION || w.band == PressureBand.CRITICAL) {
                collisionsArr.put(JSONObject().apply {
                    put("week", w.weekNumber)
                    put("hours", (Math.round(w.totalHours * 10.0) / 10.0))
                    put("capacity", semester.capacityHoursPerWeek.toInt())
                    put("cause", w.collisionExplanation ?: w.collisionHeadline)
                })
            }
        }
        root.put("load", loadArr)
        root.put("collisions", collisionsArr)

        return root.toString(2)
    }

    /**
     * Checks if the question mentions a course and words like late, extension, absence, resit, penalty, policy.
     * If so, retrieves that course's stored syllabus policy text and source citation.
     */
    fun retrieveOnDemandSyllabusPolicies(
        query: String,
        courses: List<Course>
    ): String? {
        val lowerQuery = query.lowercase(Locale.ROOT)
        val policyKeywords = listOf(
            "late", "extension", "absence", "absent", "attend", "attendance",
            "resit", "penalty", "policy", "slip", "grace", "hand in", "submit late", "makeup", "make-up"
        )

        val hasPolicyIntent = policyKeywords.any { lowerQuery.contains(it) }
        if (!hasPolicyIntent) return null

        val matchedCourses = courses.filter { c ->
            val codeClean = c.code.lowercase(Locale.ROOT).replace(" ", "")
            val titleWords = c.title.lowercase(Locale.ROOT).split(" ").filter { it.length > 3 }
            lowerQuery.contains(c.code.lowercase(Locale.ROOT)) ||
                lowerQuery.contains(codeClean) ||
                titleWords.any { lowerQuery.contains(it) }
        }

        val targetCourses = if (matchedCourses.isNotEmpty()) matchedCourses else courses

        val sb = StringBuilder()
        sb.append("ON-DEMAND SYLLABUS POLICY RETRIEVAL:\n")
        var foundAny = false

        for (c in targetCourses) {
            val late = c.policies.late
            val att = c.policies.attendance
            val resit = c.policies.resubmission
            val quote = c.source.quote
            val page = c.source.page
            val fileName = c.source.fileName

            if (late != null || att != null || resit != null || quote.isNotBlank()) {
                foundAny = true
                sb.append("Course: ${c.code} (${c.title})\n")
                sb.append("Source Document: $fileName, Page $page\n")
                if (!late.isNullOrBlank()) {
                    sb.append("Late Submission Policy: \"$late\" (Page $page)\n")
                }
                if (!att.isNullOrBlank()) {
                    sb.append("Attendance Policy: \"$att\" (Page $page)\n")
                }
                if (!resit.isNullOrBlank()) {
                    sb.append("Resubmission Policy: \"$resit\" (Page $page)\n")
                }
                if (quote.isNotBlank() && late == null && att == null && resit == null) {
                    sb.append("Syllabus Excerpt: \"$quote\" (Page $page)\n")
                }
                sb.append("\n")
            }
        }

        return if (foundAny) sb.toString().trim() else null
    }

    /**
     * Detects what-if requests (e.g., extensions, date changes, capacity changes),
     * deterministically runs LoadModelEngine.computeSemesterLoad with modified data,
     * and returns the exact recomputed numbers so the model can narrate them.
     */
    fun simulateWhatIfScenario(
        query: String,
        semester: SemesterEntity,
        courses: List<Course>,
        assessments: List<Assessment>,
        baselineLoad: SemesterLoadResult?
    ): String? {
        val lowerQuery = query.lowercase(Locale.ROOT)
        if (!lowerQuery.contains("what if") &&
            !lowerQuery.contains("what-if") &&
            !lowerQuery.contains("if i get") &&
            !lowerQuery.contains("if i ask for") &&
            !lowerQuery.contains("if i move") &&
            !lowerQuery.contains("extension on") &&
            !lowerQuery.contains("postpone")
        ) {
            return null
        }

        // Try to match target assessment
        val targetAssessment = assessments.find { a ->
            val titleLower = a.title.lowercase(Locale.ROOT)
            lowerQuery.contains(titleLower) ||
                (titleLower.length > 4 && lowerQuery.contains(titleLower.take(6)))
        } ?: assessments.find { a ->
            val course = courses.find { it.id == a.courseId }
            val courseCode = course?.code?.lowercase(Locale.ROOT) ?: ""
            courseCode.isNotBlank() && lowerQuery.contains(courseCode)
        }

        if (targetAssessment == null || targetAssessment.dueDate == null) {
            return null
        }

        // Extract days offset if mentioned
        val daysOffset = when {
            lowerQuery.contains("3-day") || lowerQuery.contains("3 day") || lowerQuery.contains("three day") || lowerQuery.contains("3 days") || lowerQuery.contains("three days") -> 3
            lowerQuery.contains("2-day") || lowerQuery.contains("2 day") || lowerQuery.contains("two day") || lowerQuery.contains("2 days") -> 2
            lowerQuery.contains("4-day") || lowerQuery.contains("4 day") || lowerQuery.contains("four day") || lowerQuery.contains("4 days") -> 4
            lowerQuery.contains("5-day") || lowerQuery.contains("5 day") || lowerQuery.contains("five day") || lowerQuery.contains("5 days") -> 5
            lowerQuery.contains("7-day") || lowerQuery.contains("1 week") || lowerQuery.contains("one week") || lowerQuery.contains("a week") -> 7
            else -> 3
        }

        val originalDueDate = try {
            LocalDate.parse(targetAssessment.dueDate)
        } catch (e: Exception) {
            return null
        }

        val newDueDate = originalDueDate.plusDays(daysOffset.toLong())

        val modifiedAssessments = assessments.map { a ->
            if (a.id == targetAssessment.id) {
                a.copy(dueDate = newDueDate.toString())
            } else {
                a
            }
        }

        val recomputed = LoadModelEngine.computeSemesterLoad(
            semesterId = semester.id,
            startDateIso = semester.startDate,
            weekCount = semester.weekCount,
            breaks = emptyList(),
            capacityHoursPerWeek = semester.capacityHoursPerWeek,
            courses = courses,
            assessments = modifiedAssessments
        )

        val targetCourse = courses.find { it.id == targetAssessment.courseId }
        val courseCode = targetCourse?.code ?: "COURSE"

        val sb = StringBuilder()
        sb.append("DETERMINISTIC WHAT-IF RECOMPUTE RESULTS:\n")
        sb.append("Assumption tested: ${courseCode} ${targetAssessment.title} moved from ${targetAssessment.dueDate} to $newDueDate (+$daysOffset days extension).\n")

        // Find affected weeks
        val baselineWeeks = baselineLoad?.weeks ?: emptyList()
        val affectedWeeks = mutableListOf<String>()

        recomputed.weeks.forEach { rw ->
            val bw = baselineWeeks.find { it.weekNumber == rw.weekNumber }
            val oldHours = bw?.totalHours ?: 0.0
            val newHours = rw.totalHours
            if (Math.abs(oldHours - newHours) > 0.05) {
                val oldHoursStr = String.format(Locale.ENGLISH, "%.1fh", oldHours)
                val newHoursStr = String.format(Locale.ENGLISH, "%.1fh", newHours)
                val oldBand = bw?.band?.label ?: "steady"
                val newBand = rw.band.label
                affectedWeeks.add("Week ${rw.weekNumber}: load changes from $oldHoursStr ($oldBand) to $newHoursStr ($newBand)")
            }
        }

        if (affectedWeeks.isNotEmpty()) {
            affectedWeeks.forEach { sb.append("· $it\n") }
        } else {
            sb.append("Load shifts slightly within current window without altering peak weekly pressure.\n")
        }

        return sb.toString().trim()
    }
}
