package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChatMessage
import com.example.data.MessageSender
import com.example.data.SemesterChatService
import com.example.data.SemesterEntity
import com.example.model.Assessment
import com.example.model.AssessmentStatus
import com.example.model.AssessmentType
import com.example.model.Course
import com.example.model.CoursePolicies
import com.example.model.DateBasis
import com.example.model.GradeBreakdownItem
import com.example.model.HoursBasis
import com.example.model.LoadModelEngine
import com.example.model.SemesterDigestBuilder
import com.example.model.SourceRef
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SemesterChatTest {

    private lateinit var context: Context
    private lateinit var chatService: SemesterChatService

    private val testSemester = SemesterEntity(
        id = "sem_fall_2026",
        name = "Fall 2026",
        startDate = "2026-08-31",
        weekCount = 16,
        breaksJson = "[]",
        capacityHoursPerWeek = 25.0,
        createdAt = "2026-08-01"
    )

    private val testCourses = listOf(
        Course(
            id = "c_pol",
            semesterId = "sem_fall_2026",
            code = "POL 201",
            title = "Comparative Politics",
            instructor = "Dr. Alvarez",
            credits = 4.0,
            gradeBreakdown = listOf(
                GradeBreakdownItem("Essays", 40.0),
                GradeBreakdownItem("Final Exam", 40.0),
                GradeBreakdownItem("Participation", 20.0)
            ),
            policies = CoursePolicies(
                late = "Late papers are penalized 5% per calendar day, up to 3 days maximum. No credit after 72 hours.",
                attendance = "Two unexcused absences permitted before a half-letter grade penalty.",
                resubmission = null
            ),
            source = SourceRef("pol201.pdf", 4, "POL 201 Syllabus"),
            confidence = 0.95
        ),
        Course(
            id = "c_chem",
            semesterId = "sem_fall_2026",
            code = "CHEM 101",
            title = "General Chemistry I",
            instructor = "Prof. Zhang",
            credits = 4.0,
            gradeBreakdown = listOf(
                GradeBreakdownItem("Lab Reports", 30.0),
                GradeBreakdownItem("Midterms", 40.0),
                GradeBreakdownItem("Final Exam", 30.0)
            ),
            policies = CoursePolicies(
                late = null, // No late policy stated in CHEM
                attendance = "Mandatory lab attendance. Missed labs receive a zero.",
                resubmission = null
            ),
            source = SourceRef("chem101.pdf", 2, "General Chemistry"),
            confidence = 0.95
        )
    )

    private val testAssessments = listOf(
        Assessment(
            id = "a_pol_essay1",
            courseId = "c_pol",
            title = "Essay 1",
            type = AssessmentType.ESSAY,
            weightPercent = 20.0,
            dueDate = "2026-10-15", // Thursday of Week 7
            dateBasis = DateBasis.STATED,
            estimatedHours = 14.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("pol201.pdf", 4, "Essay 1"),
            confidence = 0.95,
            isConfirmed = true
        ),
        Assessment(
            id = "a_chem_midterm",
            courseId = "c_chem",
            title = "Midterm Exam 1",
            type = AssessmentType.MIDTERM,
            weightPercent = 20.0,
            dueDate = "2026-10-16", // Friday of Week 7
            dateBasis = DateBasis.STATED,
            estimatedHours = 18.0,
            hoursBasis = HoursBasis.DEFAULT,
            status = AssessmentStatus.TODO,
            source = SourceRef("chem101.pdf", 3, "Midterm 1"),
            confidence = 0.95,
            isConfirmed = true
        )
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        chatService = SemesterChatService(context)
    }

    @Test
    fun testSemesterDigestJsonStructure() {
        val loadResult = LoadModelEngine.computeSemesterLoad(
            semesterId = testSemester.id,
            startDateIso = testSemester.startDate,
            weekCount = testSemester.weekCount,
            breaks = emptyList(),
            capacityHoursPerWeek = testSemester.capacityHoursPerWeek,
            courses = testCourses,
            assessments = testAssessments
        )

        val digestJson = SemesterDigestBuilder.buildDigest(
            semester = testSemester,
            courses = testCourses,
            assessments = testAssessments,
            loadResult = loadResult
        )

        val json = JSONObject(digestJson)
        assertTrue(json.has("semester"))
        assertTrue(json.has("courses"))
        assertTrue(json.has("assessments"))
        assertTrue(json.has("load"))
        assertTrue(json.has("collisions"))

        val sem = json.getJSONObject("semester")
        assertTrue(sem.has("week"))
        assertTrue(sem.has("of"))
        assertTrue(sem.has("capacity"))

        val courses = json.getJSONArray("courses")
        assertTrue(courses.length() == 2)
        val firstCourse = courses.getJSONObject(0)
        assertTrue(firstCourse.has("code"))
        assertTrue(firstCourse.has("breakdown"))
        assertTrue(firstCourse.has("policies"))
    }

    @Test
    fun testOnDemandPolicyRetrieval() {
        // Inquiry with course code and policy keyword
        val policyResult = SemesterDigestBuilder.retrieveOnDemandSyllabusPolicies(
            "Can I hand in the POL 201 essay late?",
            testCourses
        )
        assertNotNull(policyResult)
        assertTrue(policyResult!!.contains("POL 201"))
        assertTrue(policyResult.contains("Late papers are penalized 5% per calendar day"))
        assertTrue(policyResult.contains("Page 4"))

        // Unrelated inquiry should return null
        val noPolicyResult = SemesterDigestBuilder.retrieveOnDemandSyllabusPolicies(
            "What should I do today?",
            testCourses
        )
        assertTrue(noPolicyResult == null)
    }

    @Test
    fun testWhatIfScenarioDeterministicRecompute() {
        val loadResult = LoadModelEngine.computeSemesterLoad(
            semesterId = testSemester.id,
            startDateIso = testSemester.startDate,
            weekCount = testSemester.weekCount,
            breaks = emptyList(),
            capacityHoursPerWeek = testSemester.capacityHoursPerWeek,
            courses = testCourses,
            assessments = testAssessments
        )

        val whatIfSummary = SemesterDigestBuilder.simulateWhatIfScenario(
            "What if I ask for a 3-day extension on Essay 1?",
            testSemester,
            testCourses,
            testAssessments,
            loadResult
        )

        assertNotNull(whatIfSummary)
        assertTrue(whatIfSummary!!.contains("POL 201 Essay 1"))
        assertTrue(whatIfSummary.contains("+3 days extension"))
    }

    @Test
    fun testDeterministicReplyForPolicyLookup_PresentAndMissing() {
        val loadResult = LoadModelEngine.computeSemesterLoad(
            semesterId = testSemester.id,
            startDateIso = testSemester.startDate,
            weekCount = testSemester.weekCount,
            breaks = emptyList(),
            capacityHoursPerWeek = testSemester.capacityHoursPerWeek,
            courses = testCourses,
            assessments = testAssessments
        )

        // 1. Present in syllabus: quotes text and cites page
        val presentReply = chatService.generateDeterministicReply(
            query = "Can I hand in the POL 201 paper late?",
            semester = testSemester,
            courses = testCourses,
            assessments = testAssessments,
            loadResult = loadResult,
            onDemandPolicy = null,
            whatIfSimulation = null
        )
        assertTrue(presentReply.contains("POL 201 syllabus (page 4) states:"))
        assertTrue(presentReply.contains("Late papers are penalized 5%"))
        assertFalse(presentReply.contains("!")) // No exclamation marks per prompt rules

        // 2. Missing in syllabus: honest refusal directing to instructor
        val missingReply = chatService.generateDeterministicReply(
            query = "Can I submit late in CHEM 101?",
            semester = testSemester,
            courses = testCourses,
            assessments = testAssessments,
            loadResult = loadResult,
            onDemandPolicy = null,
            whatIfSimulation = null
        )
        assertTrue(missingReply.contains("That isn't in the CHEM 101 syllabus. Ask the instructor."))
    }

    @Test
    fun testDeterministicReplyForTriageAndFraming() {
        val loadResult = LoadModelEngine.computeSemesterLoad(
            semesterId = testSemester.id,
            startDateIso = testSemester.startDate,
            weekCount = testSemester.weekCount,
            breaks = emptyList(),
            capacityHoursPerWeek = testSemester.capacityHoursPerWeek,
            courses = testCourses,
            assessments = testAssessments
        )

        // Triage: "What should I start today?"
        val triageReply = chatService.generateDeterministicReply(
            query = "What should I start today?",
            semester = testSemester,
            courses = testCourses,
            assessments = testAssessments,
            loadResult = loadResult,
            onDemandPolicy = null,
            whatIfSimulation = null
        )
        assertTrue(triageReply.contains("POL 201 Essay 1"))
        assertTrue(triageReply.contains("due 2026-10-15"))
        assertTrue(triageReply.contains("20%"))

        // Framing: "Is week 7 as bad as it looks?"
        val framingReply = chatService.generateDeterministicReply(
            query = "Is week 7 as bad as it looks?",
            semester = testSemester,
            courses = testCourses,
            assessments = testAssessments,
            loadResult = loadResult,
            onDemandPolicy = null,
            whatIfSimulation = null
        )
        assertTrue(framingReply.contains("Week 7 is"))
        assertTrue(framingReply.contains("against your 25 capacity"))
    }
}
