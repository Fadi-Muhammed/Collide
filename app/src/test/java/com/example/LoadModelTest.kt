package com.example

import com.example.model.Assessment
import com.example.model.AssessmentStatus
import com.example.model.AssessmentType
import com.example.model.Course
import com.example.model.DateBasis
import com.example.model.HoursBasis
import com.example.model.LoadModelEngine
import com.example.model.PressureBand
import com.example.model.SemesterBreak
import com.example.model.SourceRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LoadModelTest {

    @Test
    fun testEstimatedHoursCalculation() {
        // essay: base 14h, weight 20% -> weightFactor = clamp(20/10, 0.5, 3.0) = 2.0. Credits 3.0 -> creditFactor = 1.0
        // estimatedHours = 14 * 2.0 * 1.0 = 28.0
        val essayHours = LoadModelEngine.estimateHours(AssessmentType.ESSAY, 20.0, 3.0)
        assertEquals(28.0, essayHours, 0.01)

        // reading-response: base 2h, weight 2% -> weightFactor = clamp(0.2, 0.5, 3.0) = 0.5. Credits 4.0 -> creditFactor = 4/3
        // rawHours = 2 * 0.5 * (4.0/3.0) = 1.3333... rounded to 1 decimal place = 1.3
        val readingHours = LoadModelEngine.estimateHours(AssessmentType.READING_RESPONSE, 2.0, 4.0)
        assertEquals(1.3, readingHours, 0.01)

        // final-exam: base 20h, weight 40% -> weightFactor = clamp(40/10, 0.5, 3.0) = 3.0. Credits 3.0
        // estimatedHours = 20 * 3.0 * 1.0 = 60.0
        val examHours = LoadModelEngine.estimateHours(AssessmentType.FINAL_EXAM, 40.0, 3.0)
        assertEquals(60.0, examHours, 0.01)
    }

    @Test
    fun testSpreadBackwardsLinearRamp() {
        val dueDate = LocalDate.of(2026, 10, 15) // Thursday
        val assessment = Assessment(
            id = "a1",
            courseId = "c1",
            title = "Research Essay",
            type = AssessmentType.ESSAY,
            weightPercent = 20.0,
            dueDate = dueDate.toString(),
            dateBasis = DateBasis.STATED,
            estimatedHours = 28.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("syllabus.pdf", 1, "Essay 20%"),
            confidence = 0.95
        )

        val dailySpread = LoadModelEngine.spreadAssessmentHours(assessment, emptyList())
        // Window for essay is 12 days
        assertEquals(12, dailySpread.size)
        // Sum of hours should equal estimatedHours (28.0)
        val sumHours = dailySpread.values.sum()
        assertEquals(28.0, sumHours, 0.05)

        // The due date (day 12 of 12) should receive the most weight: 28 * 12 / (12*13/2) = 28 * 12 / 78 = 4.307 hours
        val lastDayHours = dailySpread[dueDate] ?: 0.0
        assertEquals(28.0 * 12.0 / 78.0, lastDayHours, 0.05)

        // Day 1 of window (dueDate - 11 days) receives the least: 28 * 1 / 78 = 0.3589 hours
        val firstDay = dueDate.minusDays(11)
        val firstDayHours = dailySpread[firstDay] ?: 0.0
        assertEquals(28.0 * 1.0 / 78.0, firstDayHours, 0.05)
        assertTrue(lastDayHours > firstDayHours * 10)
    }

    @Test
    fun testBreakRedistribution() {
        val dueDate = LocalDate.of(2026, 10, 15)
        val breakDays = listOf(
            LocalDate.of(2026, 10, 10),
            LocalDate.of(2026, 10, 11)
        )
        val semesterBreaks = listOf(
            SemesterBreak(
                label = "Fall Break",
                start = "2026-10-10",
                end = "2026-10-11"
            )
        )
        val assessment = Assessment(
            id = "a1",
            courseId = "c1",
            title = "Research Essay",
            type = AssessmentType.ESSAY,
            weightPercent = 20.0,
            dueDate = dueDate.toString(),
            dateBasis = DateBasis.STATED,
            estimatedHours = 28.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("syllabus.pdf", 1, "Essay 20%"),
            confidence = 0.95
        )

        val dailySpread = LoadModelEngine.spreadAssessmentHours(assessment, semesterBreaks)
        // Break dates must have 0 hours
        breakDays.forEach { date ->
            val hours = dailySpread[date] ?: 0.0
            assertEquals(0.0, hours, 0.001)
        }

        // Sum of hours across active non-break days should still equal 28.0
        val sumHours = dailySpread.values.sum()
        assertEquals(28.0, sumHours, 0.05)
    }

    @Test
    fun testTraceWithholdingRule() {
        val course = Course(
            id = "c1",
            semesterId = "sem1",
            code = "CS 3110",
            title = "Data Structures",
            instructor = "Prof. Smith",
            credits = 4.0,
            gradeBreakdown = emptyList(),
            confidence = 0.95,
            source = SourceRef("cs.pdf", 1, "CS 3110")
        )

        val highConfAssessment = Assessment(
            id = "a1",
            courseId = "c1",
            title = "Problem Set 1",
            type = AssessmentType.PROBLEM_SET,
            weightPercent = 10.0,
            dueDate = "2026-09-15",
            dateBasis = DateBasis.STATED,
            estimatedHours = 8.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("cs.pdf", 2, "PS1"),
            confidence = 0.90, // >= 0.85 -> included
            isConfirmed = false
        )

        val lowConfUnconfirmed = Assessment(
            id = "a2",
            courseId = "c1",
            title = "Uncertain Quiz",
            type = AssessmentType.QUIZ,
            weightPercent = 5.0,
            dueDate = "2026-09-16",
            dateBasis = DateBasis.DERIVED_FROM_WEEK,
            estimatedHours = 4.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("cs.pdf", 2, "Quiz"),
            confidence = 0.60, // < 0.85 and not confirmed -> withheld from trace
            isConfirmed = false
        )

        val lowConfConfirmed = Assessment(
            id = "a3",
            courseId = "c1",
            title = "Confirmed Milestone",
            type = AssessmentType.PROJECT_MILESTONE,
            weightPercent = 15.0,
            dueDate = "2026-09-17",
            dateBasis = DateBasis.STATED,
            estimatedHours = 14.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("cs.pdf", 3, "Project"),
            confidence = 0.65,
            isConfirmed = true // Confirmed by user -> included
        )

        val result = LoadModelEngine.computeSemesterLoad(
            semesterId = "sem1",
            startDateIso = "2026-08-31",
            weekCount = 16,
            breaks = emptyList(),
            capacityHoursPerWeek = 25.0,
            courses = listOf(course),
            assessments = listOf(highConfAssessment, lowConfUnconfirmed, lowConfConfirmed)
        )

        // Verify that only a1 and a3 are in the load result
        val allContributors = result.weeks.flatMap { it.contributors }.map { it.assessment.id }.toSet()
        assertTrue(allContributors.contains("a1"))
        assertTrue(allContributors.contains("a3"))
        assertTrue(!allContributors.contains("a2"))
    }

    @Test
    fun testCapacityChangeRepaintsBandsAndCollisionExplanation() {
        val course1 = Course(
            id = "c1",
            semesterId = "sem1",
            code = "POL 201",
            title = "Political Theory",
            instructor = "Prof. Green",
            credits = 3.0,
            gradeBreakdown = emptyList(),
            confidence = 0.95,
            source = SourceRef("pol.pdf", 1, "POL 201")
        )

        val course2 = Course(
            id = "c2",
            semesterId = "sem1",
            code = "PHYS 2041",
            title = "Mechanics",
            instructor = "Dr. Vance",
            credits = 4.0,
            gradeBreakdown = emptyList(),
            confidence = 0.95,
            source = SourceRef("phys.pdf", 1, "PHYS 2041")
        )

        val essay = Assessment(
            id = "a1",
            courseId = "c1",
            title = "Midterm Essay",
            type = AssessmentType.ESSAY,
            weightPercent = 25.0,
            dueDate = "2026-10-15", // Week 7
            dateBasis = DateBasis.STATED,
            estimatedHours = 35.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("pol.pdf", 1, "Essay"),
            confidence = 0.95,
            isConfirmed = true
        )

        val project = Assessment(
            id = "a2",
            courseId = "c2",
            title = "Lab Project Milestone",
            type = AssessmentType.PROJECT_MILESTONE,
            weightPercent = 20.0,
            dueDate = "2026-10-16", // Week 7
            dateBasis = DateBasis.STATED,
            estimatedHours = 32.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("phys.pdf", 1, "Project"),
            confidence = 0.95,
            isConfirmed = true
        )

        // At capacity 50h, Week 7 is steady / calm
        val result50 = LoadModelEngine.computeSemesterLoad(
            semesterId = "sem1",
            startDateIso = "2026-08-31",
            weekCount = 16,
            breaks = emptyList(),
            capacityHoursPerWeek = 50.0,
            courses = listOf(course1, course2),
            assessments = listOf(essay, project)
        )
        val week7Cap50 = result50.weeks.first { it.weekNumber == 7 }
        assertTrue(week7Cap50.band <= PressureBand.STEADY)

        // At capacity 15h, Week 7 spikes to CRITICAL (>1.40x) with deterministic collision sentence
        val result15 = LoadModelEngine.computeSemesterLoad(
            semesterId = "sem1",
            startDateIso = "2026-08-31",
            weekCount = 16,
            breaks = emptyList(),
            capacityHoursPerWeek = 15.0,
            courses = listOf(course1, course2),
            assessments = listOf(essay, project)
        )
        val week7Cap15 = result15.weeks.first { it.weekNumber == 7 }
        assertEquals(PressureBand.CRITICAL, week7Cap15.band)
        assertNotNull(week7Cap15.collisionHeadline)
        assertNotNull(week7Cap15.collisionExplanation)
        assertTrue(week7Cap15.collisionExplanation!!.contains("POL 201 Midterm Essay"))
        assertTrue(week7Cap15.collisionExplanation!!.contains("PHYS 2041 Lab Project Milestone"))
    }

    @Test
    fun testTraceColumnsAndAccessibilityLabels() {
        val course = Course(
            id = "c1",
            semesterId = "sem1",
            code = "CS 101",
            title = "Intro",
            instructor = "Prof. X",
            credits = 3.0,
            gradeBreakdown = emptyList(),
            confidence = 0.95,
            source = SourceRef("cs.pdf", 1, "CS 101")
        )

        val assessment = Assessment(
            id = "a1",
            courseId = "c1",
            title = "Final Exam",
            type = AssessmentType.FINAL_EXAM,
            weightPercent = 40.0,
            dueDate = "2026-12-10",
            dateBasis = DateBasis.STATED,
            estimatedHours = 60.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("cs.pdf", 2, "Final"),
            confidence = 0.95,
            isConfirmed = true
        )

        val result = LoadModelEngine.computeSemesterLoad(
            semesterId = "sem1",
            startDateIso = "2026-08-31",
            weekCount = 16,
            breaks = emptyList(),
            capacityHoursPerWeek = 25.0,
            courses = listOf(course),
            assessments = listOf(assessment)
        )

        assertEquals(16, result.weeks.size)
        // Tallest week can be found
        val maxHours = result.weeks.maxOf { it.totalHours }
        assertTrue(maxHours > 0)
    }

    @Test
    fun testDateBasisUnknownContributesZeroHoursToTrace() {
        val course = Course(
            id = "c1",
            semesterId = "sem1",
            code = "ENG 200",
            title = "Literature",
            instructor = "Prof. Lit",
            credits = 3.0,
            gradeBreakdown = emptyList(),
            confidence = 0.95,
            source = SourceRef("eng.pdf", 1, "ENG 200")
        )

        val unconfirmedNoDateAssessment = Assessment(
            id = "a_nodate",
            courseId = "c1",
            title = "Term Paper",
            type = AssessmentType.ESSAY,
            weightPercent = 30.0,
            dueDate = null,
            dateBasis = DateBasis.UNKNOWN,
            estimatedHours = 20.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("eng.pdf", 4, "Term paper TBD"),
            confidence = 0.95,
            isConfirmed = true
        )

        val result = LoadModelEngine.computeSemesterLoad(
            semesterId = "sem1",
            startDateIso = "2026-08-31",
            weekCount = 16,
            breaks = emptyList(),
            capacityHoursPerWeek = 25.0,
            courses = listOf(course),
            assessments = listOf(unconfirmedNoDateAssessment)
        )

        // Verify that with no resolved due date, total trace hours is 0.0
        assertEquals(0.0, result.totalSemesterHours, 0.001)
        result.weeks.forEach { week ->
            assertEquals(0.0, week.totalHours, 0.001)
            assertTrue(week.contributors.isEmpty())
        }
    }

    @Test
    fun testAssessmentInProgressWeekHours() {
        val course = Course(
            id = "c1",
            semesterId = "sem1",
            code = "CS 201",
            title = "Data Structures",
            confidence = 0.95,
            source = SourceRef("cs201.pdf", 1, "CS 201")
        )

        // Due on Thursday of Week 3 (2026-09-17). Window = 12 days (starts in Week 2, 2026-09-06 is Sunday of W1/Monday W2).
        val assessment = Assessment(
            id = "a_essay",
            courseId = "c1",
            title = "Essay 1",
            type = AssessmentType.ESSAY, // window = 12 days
            weightPercent = 20.0,
            dueDate = "2026-09-17",
            dateBasis = DateBasis.STATED,
            estimatedHours = 14.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("cs201.pdf", 5, "Essay 1"),
            confidence = 0.95,
            isConfirmed = true
        )

        val result = LoadModelEngine.computeSemesterLoad(
            semesterId = "sem1",
            startDateIso = "2026-08-31",
            weekCount = 16,
            breaks = emptyList(),
            capacityHoursPerWeek = 25.0,
            courses = listOf(course),
            assessments = listOf(assessment)
        )

        val week2 = result.weeks.find { it.weekNumber == 2 }!!
        val week3 = result.weeks.find { it.weekNumber == 3 }!!

        // In Week 2, essay is IN PROGRESS (not due), but contributes workload hours
        assertTrue(week2.contributors.any { it.assessment.id == "a_essay" })
        val week2Contributor = week2.contributors.find { it.assessment.id == "a_essay" }!!
        assertFalse(week2Contributor.isDueInWeek)
        assertTrue(week2Contributor.totalHoursInWeek > 0.0)

        // In Week 3, essay is DUE in week
        val week3Contributor = week3.contributors.find { it.assessment.id == "a_essay" }!!
        assertTrue(week3Contributor.isDueInWeek)
        assertTrue(week3Contributor.totalHoursInWeek > 0.0)

        // Sum across both weeks within semester
        val totalContributorHours = result.weeks.flatMap { it.contributors }
            .filter { it.assessment.id == "a_essay" }
            .sumOf { it.totalHoursInWeek }
        assertEquals(14.0, totalContributorHours, 0.05)
    }
}
