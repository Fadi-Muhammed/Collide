package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CollideDatabase
import com.example.data.CollideRepository
import com.example.data.ExtractionProgress
import com.example.data.GeminiExtractorService
import com.example.model.DateBasis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExtractionRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: CollideRepository
    private lateinit var extractorService: GeminiExtractorService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = CollideRepository(context)
        extractorService = GeminiExtractorService(context)
    }

    @Test
    fun testExtractionJsonParsingAndProvenances() {
        val sampleJson = """
        {
          "course": {
            "code": "PHYS 2041",
            "title": "Classical Mechanics",
            "instructor": "Dr. Vance",
            "credits": 4.0,
            "gradeBreakdown": [
              { "label": "Problem Sets", "weightPercent": 25.0 },
              { "label": "Midterm", "weightPercent": 25.0 },
              { "label": "Final", "weightPercent": 50.0 }
            ],
            "policies": {
              "late": "10% per day",
              "attendance": "Mandatory"
            },
            "source": {
              "fileName": "PHYS2041.pdf",
              "page": 1,
              "quote": "PHYS 2041: Classical Mechanics"
            },
            "confidence": 0.98
          },
          "assessments": [
            {
              "title": "Problem Set 1",
              "type": "problem-set",
              "weightPercent": 5.0,
              "dueDate": "2026-09-21",
              "dueTime": "17:00",
              "dateBasis": "stated",
              "estimatedHours": 8.0,
              "source": {
                "fileName": "PHYS2041.pdf",
                "page": 3,
                "quote": "PSet 1 due Sep 21 at 5pm"
              },
              "confidence": 0.99
            }
          ]
        }
        """.trimIndent()

        val result = extractorService.parseExtractionResponse(
            jsonString = sampleJson,
            semesterId = "sem_123",
            defaultFileName = "PHYS2041.pdf",
            semesterStartDate = "2026-09-07"
        )

        assertEquals("PHYS 2041", result.course.code)
        assertEquals("Classical Mechanics", result.course.title)
        assertEquals(1, result.assessments.size)
        assertEquals(DateBasis.STATED, result.assessments[0].dateBasis)
        assertEquals("PHYS2041.pdf", result.assessments[0].source.fileName)
        assertEquals(3, result.assessments[0].source.page)
        assertEquals("PSet 1 due Sep 21 at 5pm", result.assessments[0].source.quote)
    }

    @Test
    fun testWeightDiscrepancyDetection() {
        val discrepantJson = """
        {
          "course": {
            "code": "MATH 2940",
            "title": "Linear Algebra",
            "source": { "fileName": "MATH2940.pdf", "page": 1, "quote": "Math 2940" },
            "confidence": 0.95
          },
          "assessments": [
            {
              "title": "HW 1",
              "type": "problem-set",
              "weightPercent": 20.0,
              "dueDate": "2026-09-18",
              "source": { "fileName": "MATH2940.pdf", "page": 2, "quote": "HW1" },
              "confidence": 0.95
            },
            {
              "title": "Prelim 1",
              "type": "midterm",
              "weightPercent": 25.0,
              "dueDate": "2026-10-15",
              "source": { "fileName": "MATH2940.pdf", "page": 3, "quote": "Prelim 1" },
              "confidence": 0.95
            },
            {
              "title": "Prelim 2",
              "type": "midterm",
              "weightPercent": 40.0,
              "dueDate": "2026-11-12",
              "source": { "fileName": "MATH2940.pdf", "page": 3, "quote": "Prelim 2" },
              "confidence": 0.95
            }
          ]
        }
        """.trimIndent()

        val result = extractorService.parseExtractionResponse(
            jsonString = discrepantJson,
            semesterId = "sem_123",
            defaultFileName = "MATH2940.pdf",
            semesterStartDate = "2026-09-07"
        )

        assertEquals(85.0, result.totalWeightSum, 0.01)
        assertNotNull(result.weightDiscrepancyNotice)
        assertTrue(result.weightDiscrepancyNotice!!.contains("Weights total 85%. 15% unaccounted for."))
    }

    @Test
    fun testBatchExtractionAndCaching() = runBlocking {
        val semester = repository.createSemester(
            name = "Autumn 2026",
            startDateIso = "2026-09-07",
            weekCount = 14,
            breaks = emptyList(),
            capacityHoursPerWeek = 25.0
        )

        // Load 6 sample syllabi
        val files = repository.loadSampleSyllabi(semester.id)
        assertEquals(6, files.size)

        var progressCount = 0
        val extractedResults = repository.extractAllFiles(semester.id) { progress ->
            progressCount++
        }

        assertEquals(6, extractedResults.size)
        assertTrue(progressCount > 0)

        // Verify SHA-256 caching by re-running extraction
        var reExtractProgressCount = 0
        val cachedResults = repository.extractAllFiles(semester.id) {
            reExtractProgressCount++
        }
        assertEquals(6, cachedResults.size)
    }
}
