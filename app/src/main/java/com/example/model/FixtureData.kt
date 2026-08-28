package com.example.model

object FixtureData {
    val sampleSemester = Semester(
        id = "sem-autumn-2026",
        name = "Autumn 2026",
        startDate = "2026-09-07",
        weekCount = 14,
        breaks = listOf(
            SemesterBreak(
                label = "Reading Week",
                start = "2026-10-19",
                end = "2026-10-25"
            )
        ),
        capacityHoursPerWeek = 25.0,
        createdAt = "2026-08-28T10:00:00Z"
    )

    val course1 = Course(
        id = "course-phys-2041",
        semesterId = "sem-autumn-2026",
        code = "PHYS 2041",
        title = "Classical Mechanics & Oscillations",
        instructor = "Dr. H. Vance",
        credits = 4.0,
        gradeBreakdown = listOf(
            GradeBreakdownItem("Problem Sets (5)", 25.0),
            GradeBreakdownItem("Midterm Exam", 25.0),
            GradeBreakdownItem("Lab Reports (3)", 20.0),
            GradeBreakdownItem("Final Exam", 30.0)
        ),
        policies = CoursePolicies(
            late = "10% deduction per calendar day, max 3 days late.",
            attendance = "Mandatory for weekly lab practicals."
        ),
        source = SourceRef(
            fileName = "PHYS2041_Syllabus_Fall2026.pdf",
            page = 1,
            quote = "PHYS 2041: Classical Mechanics. 4 Credit Hours. Instructor: Dr. H. Vance. 5 Problem Sets (25%), Midterm (25%), 3 Lab Reports (20%), Final Exam (30%)."
        ),
        confidence = 0.98
    )

    val course2 = Course(
        id = "course-cs-3110",
        semesterId = "sem-autumn-2026",
        code = "CS 3110",
        title = "Data Structures & Functional Programming",
        instructor = "Prof. R. Clarkson",
        credits = 4.0,
        gradeBreakdown = listOf(
            GradeBreakdownItem("Programming Assignments", 40.0),
            GradeBreakdownItem("Prelim 1", 15.0),
            GradeBreakdownItem("Prelim 2", 15.0),
            GradeBreakdownItem("Final Project", 30.0)
        ),
        policies = CoursePolicies(
            late = "Each student receives 4 slip days for the semester.",
            resubmission = "No resubmissions allowed for major prelims."
        ),
        source = SourceRef(
            fileName = "CS3110_Course_Guide.pdf",
            page = 2,
            quote = "CS 3110 Data Structures and Functional Programming. Grade distribution: 40% Assignments, 30% Prelims (2x15%), 30% Final Project."
        ),
        confidence = 0.96
    )

    val sampleCourses = listOf(course1, course2)

    val sampleAssessments = listOf(
        Assessment(
            id = "ass-phys-ps1",
            courseId = "course-phys-2041",
            title = "Problem Set 1: Lagrangian Formulation",
            type = AssessmentType.PROBLEM_SET,
            weightPercent = 5.0,
            dueDate = "2026-09-21",
            dueTime = "17:00",
            dateBasis = DateBasis.STATED,
            estimatedHours = 8.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef(
                fileName = "PHYS2041_Syllabus_Fall2026.pdf",
                page = 3,
                quote = "PSet 1 due Monday of Week 3 (Sep 21) at 5:00 PM in the physics assignment dropbox."
            ),
            confidence = 0.99
        ),
        Assessment(
            id = "ass-cs-a1",
            courseId = "course-cs-3110",
            title = "Assignment 1: Persistent Search Trees",
            type = AssessmentType.PROJECT_MILESTONE,
            weightPercent = 8.0,
            dueDate = "2026-09-24",
            dueTime = "23:59",
            dateBasis = DateBasis.STATED,
            estimatedHours = 14.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef(
                fileName = "CS3110_Course_Guide.pdf",
                page = 4,
                quote = "A1 (Search Trees) due Thursday Sep 24 by 11:59pm via CMSX submission system."
            ),
            confidence = 0.97
        )
    )
}
