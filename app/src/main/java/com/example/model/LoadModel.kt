package com.example.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Assessment Type configuration for load estimation.
 */
data class AssessmentTypeConfig(
    val type: AssessmentType,
    val baseHours: Double,
    val windowDays: Int
)

/**
 * Standard default load model lookup table.
 * "Every number here is a starting guess and every one is editable per assessment.
 * Expose them in settings as 'How long things take you', and let the student tune them once."
 */
val DEFAULT_TYPE_CONFIGS: Map<AssessmentType, AssessmentTypeConfig> = mapOf(
    AssessmentType.READING_RESPONSE to AssessmentTypeConfig(AssessmentType.READING_RESPONSE, 2.0, 3),
    AssessmentType.QUIZ to AssessmentTypeConfig(AssessmentType.QUIZ, 3.0, 4),
    AssessmentType.PROBLEM_SET to AssessmentTypeConfig(AssessmentType.PROBLEM_SET, 6.0, 6),
    AssessmentType.LAB_REPORT to AssessmentTypeConfig(AssessmentType.LAB_REPORT, 8.0, 7),
    AssessmentType.PRESENTATION to AssessmentTypeConfig(AssessmentType.PRESENTATION, 10.0, 10),
    AssessmentType.ESSAY to AssessmentTypeConfig(AssessmentType.ESSAY, 14.0, 12),
    AssessmentType.MIDTERM to AssessmentTypeConfig(AssessmentType.MIDTERM, 12.0, 14),
    AssessmentType.PROJECT_MILESTONE to AssessmentTypeConfig(AssessmentType.PROJECT_MILESTONE, 16.0, 18),
    AssessmentType.FINAL_EXAM to AssessmentTypeConfig(AssessmentType.FINAL_EXAM, 20.0, 18),
    AssessmentType.PARTICIPATION to AssessmentTypeConfig(AssessmentType.PARTICIPATION, 2.0, 7),
    AssessmentType.OTHER to AssessmentTypeConfig(AssessmentType.OTHER, 6.0, 7)
)

/**
 * Pressure Band Classification.
 */
enum class PressureBand(val label: String, val threshold: String) {
    CALM("calm", "< 0.60"),
    STEADY("steady", "0.60 – 0.89"),
    BUSY("busy", "0.90 – 1.09"),
    COLLISION("collision", "1.10 – 1.39"),
    CRITICAL("critical", "≥ 1.40")
}

fun pressureToBand(pressure: Double): PressureBand = when {
    pressure < 0.60 -> PressureBand.CALM
    pressure < 0.90 -> PressureBand.STEADY
    pressure < 1.10 -> PressureBand.BUSY
    pressure < 1.40 -> PressureBand.COLLISION
    else -> PressureBand.CRITICAL
}

/**
 * Daily distribution of an assessment's workload.
 */
data class DailyLoad(
    val date: LocalDate,
    val assessmentId: String,
    val courseId: String,
    val hours: Double
)

/**
 * A contributor to a week's load.
 */
data class WeekContributor(
    val assessment: Assessment,
    val course: Course?,
    val totalHoursInWeek: Double,
    val isDueInWeek: Boolean,
    val dueDayOfWeek: DayOfWeek?,
    val isFinalWeekOfWindow: Boolean
)

/**
 * Week Load Analysis.
 */
data class WeekLoadData(
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalHours: Double,
    val capacityHours: Double,
    val pressure: Double,
    val band: PressureBand,
    val dailyHours: Map<LocalDate, Double>,
    val contributors: List<WeekContributor>,
    val collisionHeadline: String,
    val collisionExplanation: String?
)

/**
 * Complete Semester Load Calculation Result.
 */
data class SemesterLoadResult(
    val semesterId: String,
    val capacityHoursPerWeek: Double,
    val weeks: List<WeekLoadData>,
    val maxWeekHours: Double,
    val maxPressure: Double,
    val busyOrWorseWeekCount: Int,
    val totalSemesterHours: Double
)

/**
 * Load Model Engine.
 * Deterministic. No AI. Defensible and repeatable.
 */
object LoadModelEngine {

    /**
     * Step 1: estimate hours
     * estimatedHours = baseHours[type] × weightFactor × creditFactor
     * weightFactor   = clamp(weightPercent / 10, 0.5, 3.0)
     * creditFactor   = credits ? credits / 3 : 1
     */
    fun estimateHours(
        type: AssessmentType,
        weightPercent: Double,
        credits: Double?,
        customConfigs: Map<AssessmentType, AssessmentTypeConfig> = DEFAULT_TYPE_CONFIGS
    ): Double {
        val config = customConfigs[type] ?: DEFAULT_TYPE_CONFIGS[type] ?: AssessmentTypeConfig(type, 6.0, 7)
        val weightFactor = (weightPercent / 10.0).coerceIn(0.5, 3.0)
        val creditFactor = if (credits != null && credits > 0) credits / 3.0 else 1.0
        val rawHours = config.baseHours * weightFactor * creditFactor
        return (Math.round(rawHours * 10.0) / 10.0).coerceAtLeast(0.5)
    }

    /**
     * Get window length in days for a given assessment type.
     */
    fun getWindowDays(
        type: AssessmentType,
        customConfigs: Map<AssessmentType, AssessmentTypeConfig> = DEFAULT_TYPE_CONFIGS
    ): Int {
        return (customConfigs[type] ?: DEFAULT_TYPE_CONFIGS[type])?.windowDays ?: 7
    }

    /**
     * Checks whether a specific LocalDate is inside any scheduled semester break.
     */
    fun isDateInBreak(date: LocalDate, breaks: List<SemesterBreak>): Boolean {
        for (b in breaks) {
            try {
                val start = LocalDate.parse(b.start)
                val end = LocalDate.parse(b.end)
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    return true
                }
            } catch (e: Exception) {
                // ignore malformed break dates
            }
        }
        return false
    }

    /**
     * Step 2: spread the hours backwards
     * Work does not happen on the due date. Spread estimatedHours across the window days
     * ending on the due date, weighted toward the end. Use a linear ramp where day i of n
     * (1-indexed, n = last day) gets weight i, then normalise:
     *
     * hoursOnDay(i) = estimatedHours × i / (n(n+1)/2)
     *
     * Days inside a scheduled break get zero weight and their hours redistribute to adjacent days.
     */
    fun spreadAssessmentHours(
        assessment: Assessment,
        breaks: List<SemesterBreak>,
        customConfigs: Map<AssessmentType, AssessmentTypeConfig> = DEFAULT_TYPE_CONFIGS
    ): Map<LocalDate, Double> {
        val dueDateStr = assessment.dueDate ?: return emptyMap()
        val dueDate = try {
            LocalDate.parse(dueDateStr)
        } catch (e: Exception) {
            return emptyMap()
        }

        val estimatedHours = if (assessment.estimatedHours > 0) assessment.estimatedHours else 6.0
        val windowDays = getWindowDays(assessment.type, customConfigs).coerceAtLeast(1)

        // Generate list of dates from (dueDate - windowDays + 1) to dueDate
        val days = (0 until windowDays).map { offset ->
            dueDate.minusDays((windowDays - 1 - offset).toLong())
        }

        // Calculate initial linear weights (1 to windowDays), setting break days to 0
        val weights = days.mapIndexed { index, date ->
            val dayIndex = index + 1 // 1-indexed
            if (isDateInBreak(date, breaks)) 0.0 else dayIndex.toDouble()
        }

        val totalWeight = weights.sum()

        val resultMap = mutableMapOf<LocalDate, Double>()

        if (totalWeight > 0.0) {
            days.forEachIndexed { index, date ->
                val weight = weights[index]
                if (weight > 0.0) {
                    val dayHours = estimatedHours * (weight / totalWeight)
                    resultMap[date] = dayHours
                }
            }
        } else {
            // Fallback if all window days happen to fall directly in break:
            // shift days backwards before the break
            var cursor = dueDate
            var found = 0
            val shiftedDays = mutableListOf<LocalDate>()
            while (found < windowDays) {
                if (!isDateInBreak(cursor, breaks)) {
                    shiftedDays.add(0, cursor)
                    found++
                }
                cursor = cursor.minusDays(1)
            }
            val rampSum = (windowDays * (windowDays + 1)) / 2.0
            shiftedDays.forEachIndexed { idx, date ->
                val i = idx + 1
                resultMap[date] = estimatedHours * (i / rampSum)
            }
        }

        return resultMap
    }

    /**
     * Computes complete semester workload across all weeks.
     * Note: "Nothing reaches the trace until it has been confirmed or has confidence above 0.85."
     */
    fun computeSemesterLoad(
        semesterId: String,
        startDateIso: String,
        weekCount: Int,
        breaks: List<SemesterBreak>,
        capacityHoursPerWeek: Double,
        courses: List<Course>,
        assessments: List<Assessment>,
        customConfigs: Map<AssessmentType, AssessmentTypeConfig> = DEFAULT_TYPE_CONFIGS
    ): SemesterLoadResult {
        val courseMap = courses.associateBy { it.id }

        val startMonday = try {
            val d = LocalDate.parse(startDateIso)
            if (d.dayOfWeek == DayOfWeek.MONDAY) d else d.minusDays((d.dayOfWeek.value - 1).toLong())
        } catch (e: Exception) {
            LocalDate.now()
        }

        // Filter assessments according to the strict trace rule: confirmed OR confidence >= 0.85
        val validAssessments = assessments.filter { it.isConfirmed || it.confidence >= 0.85 }

        // Spread all valid assessments
        val assessmentDailySpreads = validAssessments.associateWith { a ->
            spreadAssessmentHours(a, breaks, customConfigs)
        }

        val weeksData = mutableListOf<WeekLoadData>()
        var totalSemesterHours = 0.0
        var maxWeekHours = 0.0
        var maxPressure = 0.0
        var busyCount = 0

        for (w in 1..weekCount) {
            val weekStart = startMonday.plusWeeks((w - 1).toLong())
            val weekEnd = weekStart.plusDays(6)

            val dailyHoursMap = mutableMapOf<LocalDate, Double>()
            for (dayOffset in 0..6) {
                dailyHoursMap[weekStart.plusDays(dayOffset.toLong())] = 0.0
            }

            // Map contributors to this week
            val contributorsList = mutableListOf<WeekContributor>()

            for ((assessment, spread) in assessmentDailySpreads) {
                var weekSumForAssessment = 0.0
                var dueInWeek = false
                var dueDay: DayOfWeek? = null

                if (assessment.dueDate != null) {
                    try {
                        val d = LocalDate.parse(assessment.dueDate)
                        if (!d.isBefore(weekStart) && !d.isAfter(weekEnd)) {
                            dueInWeek = true
                            dueDay = d.dayOfWeek
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                spread.forEach { (date, hours) ->
                    if (!date.isBefore(weekStart) && !date.isAfter(weekEnd)) {
                        weekSumForAssessment += hours
                        dailyHoursMap[date] = (dailyHoursMap[date] ?: 0.0) + hours
                    }
                }

                if (weekSumForAssessment > 0.05) {
                    val windowDays = getWindowDays(assessment.type, customConfigs)
                    val isFinalWeek = dueInWeek || windowDays <= 7
                    contributorsList.add(
                        WeekContributor(
                            assessment = assessment,
                            course = courseMap[assessment.courseId],
                            totalHoursInWeek = weekSumForAssessment,
                            isDueInWeek = dueInWeek,
                            dueDayOfWeek = dueDay,
                            isFinalWeekOfWindow = isFinalWeek
                        )
                    )
                }
            }

            val weekTotalHours = dailyHoursMap.values.sum()
            val safeCapacity = capacityHoursPerWeek.coerceAtLeast(1.0)
            val pressure = weekTotalHours / safeCapacity
            val band = pressureToBand(pressure)

            if (band >= PressureBand.BUSY) {
                busyCount++
            }
            if (weekTotalHours > maxWeekHours) maxWeekHours = weekTotalHours
            if (pressure > maxPressure) maxPressure = pressure
            totalSemesterHours += weekTotalHours

            // Sort contributors by hours descending
            val sortedContributors = contributorsList.sortedByDescending { it.totalHoursInWeek }

            // Step 4: generate deterministic explanation
            val headline = "Week $w · ${Math.round(weekTotalHours)}h against ${Math.round(safeCapacity)}h capacity"
            val explanation = if (band >= PressureBand.BUSY) {
                generateCollisionExplanation(
                    weekNumber = w,
                    contributors = sortedContributors
                )
            } else null

            weeksData.add(
                WeekLoadData(
                    weekNumber = w,
                    startDate = weekStart,
                    endDate = weekEnd,
                    totalHours = weekTotalHours,
                    capacityHours = safeCapacity,
                    pressure = pressure,
                    band = band,
                    dailyHours = dailyHoursMap,
                    contributors = sortedContributors,
                    collisionHeadline = headline,
                    collisionExplanation = explanation
                )
            )
        }

        return SemesterLoadResult(
            semesterId = semesterId,
            capacityHoursPerWeek = capacityHoursPerWeek,
            weeks = weeksData,
            maxWeekHours = maxWeekHours,
            maxPressure = maxPressure,
            busyOrWorseWeekCount = busyCount,
            totalSemesterHours = totalSemesterHours
        )
    }

    /**
     * Step 4: name the collision
     * "A band is a colour. A collision needs a sentence. For any week at busy or above,
     * generate a deterministic explanation naming the two or three assessments contributing
     * the most hours:
     *
     * Week 10 · 34h against 25h capacity
     * POL 201 essay and PHYS 2041 project milestone are both in their final week, and the CHEM quiz lands on Thursday.
     *
     * Written by code from the data, not by the model. It is the same every time, which means the student can trust it."
     */
    fun generateCollisionExplanation(
        weekNumber: Int,
        contributors: List<WeekContributor>
    ): String {
        if (contributors.isEmpty()) {
            return "Heavy multi-course concentration pushes workload over capacity."
        }

        // Helper to format item name cleanly e.g. "POL 201 essay" or "PHYS 2041 project milestone"
        fun formatItemName(c: WeekContributor): String {
            val code = c.course?.code ?: "Course"
            val typeName = c.assessment.type.displayName.replace("-", " ")
            val titleLower = c.assessment.title.lowercase()
            return if (titleLower.contains(typeName)) {
                "$code ${c.assessment.title}"
            } else {
                "$code ${c.assessment.title} ($typeName)"
            }
        }

        fun formatDay(day: DayOfWeek?): String {
            return day?.getDisplayName(TextStyle.FULL, Locale.ENGLISH) ?: "the weekend"
        }

        val top = contributors.take(3)

        if (top.size == 1) {
            val item1 = top[0]
            val name1 = formatItemName(item1)
            return if (item1.isDueInWeek && item1.dueDayOfWeek != null) {
                "$name1 peaks this week, landing on ${formatDay(item1.dueDayOfWeek)}."
            } else {
                "$name1 accounts for the majority of study load this week."
            }
        }

        if (top.size == 2) {
            val item1 = top[0]
            val item2 = top[1]
            val name1 = formatItemName(item1)
            val name2 = formatItemName(item2)

            return when {
                item1.isFinalWeekOfWindow && item2.isFinalWeekOfWindow -> {
                    "$name1 and $name2 are both in their final week."
                }
                item1.isDueInWeek && item1.dueDayOfWeek != null -> {
                    "$name1 lands on ${formatDay(item1.dueDayOfWeek)}, colliding with $name2 load."
                }
                item2.isDueInWeek && item2.dueDayOfWeek != null -> {
                    "$name2 lands on ${formatDay(item2.dueDayOfWeek)}, overlapping with $name1 preparation."
                }
                else -> {
                    "$name1 and $name2 both require heavy preparation this week."
                }
            }
        }

        // 3 contributors
        val item1 = top[0]
        val item2 = top[1]
        val item3 = top[2]
        val name1 = formatItemName(item1)
        val name2 = formatItemName(item2)
        val name3 = formatItemName(item3)

        return when {
            item1.isFinalWeekOfWindow && item2.isFinalWeekOfWindow && item3.isDueInWeek -> {
                "$name1 and $name2 are both in their final week, and the $name3 lands on ${formatDay(item3.dueDayOfWeek)}."
            }
            item1.isFinalWeekOfWindow && item2.isFinalWeekOfWindow -> {
                "$name1 and $name2 are both in their final week, with overlapping $name3 preparation."
            }
            item3.isDueInWeek && item3.dueDayOfWeek != null -> {
                "$name1 and $name2 collide with heavy load, while $name3 lands on ${formatDay(item3.dueDayOfWeek)}."
            }
            else -> {
                "$name1, $name2, and $name3 all demand sustained preparation simultaneously."
            }
        }
    }
}
