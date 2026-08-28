package com.example.model

/**
 * Semester data model.
 */
data class Semester(
    val id: String,
    val name: String,              // "Autumn 2026"
    val startDate: String,         // ISO. Monday of week 1. e.g. "2026-09-07"
    val weekCount: Int,            // typically 12–16
    val breaks: List<SemesterBreak> = emptyList(),
    val capacityHoursPerWeek: Double,   // student's honest available study hours
    val createdAt: String
)

data class SemesterBreak(
    val label: String,
    val start: String,
    val end: String
)

/**
 * Course data model.
 */
data class Course(
    val id: String,
    val semesterId: String,
    val code: String,              // "PHYS 2041"
    val title: String,
    val instructor: String? = null,
    val credits: Double? = null,
    val gradeBreakdown: List<GradeBreakdownItem> = emptyList(),
    val policies: CoursePolicies = CoursePolicies(),
    val source: SourceRef,
    val confidence: Double         // 0–1, extractor's own
)

data class GradeBreakdownItem(
    val label: String,
    val weightPercent: Double
)

data class CoursePolicies(
    val late: String? = null,
    val attendance: String? = null,
    val resubmission: String? = null
)

/**
 * Assessment data model.
 */
data class Assessment(
    val id: String,
    val courseId: String,
    val title: String,
    val type: AssessmentType,
    val weightPercent: Double,
    val dueDate: String?,          // ISO date e.g. "2026-10-12"
    val dueTime: String? = null,   // "23:59"
    val dateBasis: DateBasis,
    val estimatedHours: Double,    // from the load model, student-editable
    val hoursBasis: HoursBasis,
    val status: AssessmentStatus,
    val source: SourceRef,
    val confidence: Double,
    val isConfirmed: Boolean = false
)

enum class AssessmentType(val displayName: String) {
    PROBLEM_SET("problem-set"),
    ESSAY("essay"),
    LAB_REPORT("lab-report"),
    PRESENTATION("presentation"),
    QUIZ("quiz"),
    MIDTERM("midterm"),
    FINAL_EXAM("final-exam"),
    PROJECT_MILESTONE("project-milestone"),
    READING_RESPONSE("reading-response"),
    PARTICIPATION("participation"),
    OTHER("other")
}

enum class DateBasis(val key: String) {
    STATED("stated"),
    DERIVED_FROM_WEEK("derived-from-week"),
    INFERRED("inferred"),
    UNKNOWN("unknown")
}

enum class HoursBasis(val key: String) {
    DEFAULT("default"),
    USER("user")
}

enum class AssessmentStatus(val key: String) {
    TODO("todo"),
    DOING("doing"),
    DONE("done")
}

/**
 * Mandatory provenance tracking.
 */
data class SourceRef(
    val fileName: String,
    val page: Int,
    val quote: String              // verbatim, max ~200 chars
)
