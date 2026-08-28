package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.Assessment
import com.example.model.AssessmentStatus
import com.example.model.Course
import com.example.model.SemesterDigestBuilder
import com.example.model.SemesterLoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val quotedPolicy: String? = null,
    val sourcePage: Int? = null,
    val courseCode: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    SEMESTER
}

class SemesterChatService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    private fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun sendMessage(
        userMessage: String,
        history: List<ChatMessage>,
        semester: SemesterEntity,
        courses: List<Course>,
        assessments: List<Assessment>,
        loadResult: SemesterLoadResult?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        // 1. Build semester digest
        val digestJson = SemesterDigestBuilder.buildDigest(semester, courses, assessments, loadResult)

        // 2. On-demand syllabus policy retrieval if relevant
        val onDemandPolicy = SemesterDigestBuilder.retrieveOnDemandSyllabusPolicies(userMessage, courses)

        // 3. Deterministic what-if recompute if what-if request
        val whatIfSimulation = SemesterDigestBuilder.simulateWhatIfScenario(
            userMessage,
            semester,
            courses,
            assessments,
            loadResult
        )

        // If API key is available, call Gemini 3.5 Flash
        if (apiKey.isNotBlank()) {
            try {
                val apiResponse = callGeminiChatApi(
                    apiKey = apiKey,
                    digestJson = digestJson,
                    onDemandPolicy = onDemandPolicy,
                    whatIfSimulation = whatIfSimulation,
                    history = history,
                    userMessage = userMessage
                )
                if (apiResponse.isNotBlank()) {
                    return@withContext apiResponse
                }
            } catch (e: Exception) {
                Log.w("SemesterChatService", "Gemini API call failed, falling back to deterministic engine: ${e.message}")
            }
        }

        // Deterministic grounded response engine (offline / fallback)
        generateDeterministicReply(
            query = userMessage,
            semester = semester,
            courses = courses,
            assessments = assessments,
            loadResult = loadResult,
            onDemandPolicy = onDemandPolicy,
            whatIfSimulation = whatIfSimulation
        )
    }

    private fun callGeminiChatApi(
        apiKey: String,
        digestJson: String,
        onDemandPolicy: String?,
        whatIfSimulation: String?,
        history: List<ChatMessage>,
        userMessage: String
    ): String {
        val systemPrompt = """
You have the student's full semester as structured JSON above. Answer only from it.

Cite specifics: course codes, dates, weights, hours. Never say "you have a busy week."
Say "week 10 is 34 hours against your 25."

For any policy question, quote the syllabus text you were given and name the page. If
the policy is not in the data, say so and tell the student to ask the instructor. Do
not reason about what a policy probably says.

For what-if questions, state the assumption you are testing, then read the recomputed
load numbers you are given. Do not do arithmetic yourself.

Be brief. Three sentences unless asked for more. No pep talk, no exclamation marks, no
"you've got this." The student wants the shape of the problem, not encouragement.

If the student sounds overwhelmed, the useful response is a smaller next step, not
reassurance.
        """.trimIndent()

        val contentsArray = JSONArray()

        // Context header containing the semester digest, any on-demand syllabus excerpts, and what-if simulation results
        val contextStringBuilder = StringBuilder()
        contextStringBuilder.append("SEMESTER STRUCTURED DIGEST:\n$digestJson\n\n")

        if (!onDemandPolicy.isNullOrBlank()) {
            contextStringBuilder.append("$onDemandPolicy\n\n")
        }

        if (!whatIfSimulation.isNullOrBlank()) {
            contextStringBuilder.append("$whatIfSimulation\n\n")
        }

        // Include recent history (last 6 turns)
        val recentHistory = history.takeLast(6)
        if (recentHistory.isNotEmpty()) {
            recentHistory.forEach { msg ->
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                val contentObj = JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.text) })
                    })
                }
                contentsArray.put(contentObj)
            }
        }

        // Current turn prompt combining context and user query
        val userPrompt = if (recentHistory.isEmpty()) {
            "$contextStringBuilder\nSTUDENT QUESTION:\n$userMessage"
        } else {
            if (!onDemandPolicy.isNullOrBlank() || !whatIfSimulation.isNullOrBlank()) {
                "$onDemandPolicy\n$whatIfSimulation\nSTUDENT QUESTION: $userMessage"
            } else {
                userMessage
            }
        }

        val currentTurn = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userPrompt) })
            })
        }
        contentsArray.put(currentTurn)

        val requestBodyJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("topP", 0.95)
                put("topK", 40)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw RuntimeException("Gemini API HTTP ${response.code}: $err")
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "").trim()
                }
            }
            return ""
        }
    }

    /**
     * Deterministic, grounded offline response engine implementing all four jobs precisely:
     * 1. Triage: What should I start today / do this weekend?
     * 2. What-if: Simulate extension or date modification with deterministic recompute.
     * 3. Policy lookup: Quote syllabus text + page, or honestly state it's not in the data.
     * 4. Framing: "Is week X as bad as it looks?"
     */
    fun generateDeterministicReply(
        query: String,
        semester: SemesterEntity,
        courses: List<Course>,
        assessments: List<Assessment>,
        loadResult: SemesterLoadResult?,
        onDemandPolicy: String?,
        whatIfSimulation: String?
    ): String {
        val lowerQuery = query.lowercase(Locale.ROOT)
        val courseMap = courses.associateBy { it.id }

        // Job 2: What-if
        if (!whatIfSimulation.isNullOrBlank()) {
            val lines = whatIfSimulation.lines().filter { it.isNotBlank() }
            val assumption = lines.firstOrNull { it.startsWith("Assumption tested:") }?.removePrefix("Assumption tested:")?.trim()
            val effects = lines.filter { it.startsWith("·") }.joinToString(" ") { it.removePrefix("·").trim() }

            return if (effects.isNotBlank()) {
                "Testing assumption: $assumption. $effects."
            } else {
                "Testing assumption: $assumption. Total weekly peaks remain unchanged across affected weeks."
            }
        }

        // Job 3: Policy lookup
        val policyKeywords = listOf("late", "extension", "absence", "absent", "attend", "attendance", "resit", "penalty", "policy", "hand in", "submit late")
        val isPolicyQuestion = policyKeywords.any { lowerQuery.contains(it) }

        if (isPolicyQuestion) {
            val targetCourse = courses.find { c ->
                val code = c.code.lowercase(Locale.ROOT).replace(" ", "")
                lowerQuery.contains(c.code.lowercase(Locale.ROOT)) || lowerQuery.contains(code)
            }

            if (targetCourse != null) {
                val late = targetCourse.policies.late
                val att = targetCourse.policies.attendance
                val resit = targetCourse.policies.resubmission

                if (lowerQuery.contains("late") || lowerQuery.contains("extension") || lowerQuery.contains("penalty") || lowerQuery.contains("hand in")) {
                    if (!late.isNullOrBlank()) {
                        return "${targetCourse.code} syllabus (page ${targetCourse.source.page}) states: \"$late\""
                    } else {
                        return "That isn't in the ${targetCourse.code} syllabus. Ask the instructor."
                    }
                } else if (lowerQuery.contains("attend") || lowerQuery.contains("absence") || lowerQuery.contains("absent")) {
                    if (!att.isNullOrBlank()) {
                        return "${targetCourse.code} syllabus (page ${targetCourse.source.page}) states: \"$att\""
                    } else {
                        return "That isn't in the ${targetCourse.code} syllabus. Ask the instructor."
                    }
                } else if (lowerQuery.contains("resit") || lowerQuery.contains("resubmi")) {
                    if (!resit.isNullOrBlank()) {
                        return "${targetCourse.code} syllabus (page ${targetCourse.source.page}) states: \"$resit\""
                    } else {
                        return "That isn't in the ${targetCourse.code} syllabus. Ask the instructor."
                    }
                }
            } else {
                // Check if any course matched in onDemandPolicy
                val coursesWithPolicies = courses.filter { it.policies.late != null || it.policies.attendance != null }
                if (coursesWithPolicies.isNotEmpty()) {
                    val c = coursesWithPolicies.first()
                    val pol = c.policies.late ?: c.policies.attendance
                    return "${c.code} syllabus (page ${c.source.page}) states: \"$pol\""
                } else {
                    return "No late or attendance policy is recorded in your uploaded syllabi. Ask your instructors."
                }
            }
        }

        // Job 4: Framing ("Is week X as bad as it looks?")
        val weekMatch = Regex("""week\s+(\d+)""").find(lowerQuery)
        if (weekMatch != null || lowerQuery.contains("worst week") || lowerQuery.contains("as bad as it looks") || lowerQuery.contains("collision")) {
            val targetWeekNum = weekMatch?.groupValues?.get(1)?.toIntOrNull()
            val weekData = if (targetWeekNum != null) {
                loadResult?.weeks?.find { it.weekNumber == targetWeekNum }
            } else {
                loadResult?.weeks?.maxByOrNull { it.totalHours }
            }

            if (weekData != null) {
                val cap = semester.capacityHoursPerWeek.toInt()
                val hrs = String.format(Locale.ENGLISH, "%.1f", weekData.totalHours)
                val num = weekData.weekNumber
                val cause = weekData.collisionExplanation ?: weekData.collisionHeadline
                return "Week $num is $hrs hours against your $cap capacity. $cause."
            }
        }

        // Job 1: Triage ("What should I start today?", "What do I do this weekend?")
        val today = LocalDate.now()
        val validAssessments = assessments.filter { it.dueDate != null && it.status == AssessmentStatus.TODO }
            .sortedWith(compareBy({ it.dueDate }, { -it.weightPercent }, { -it.estimatedHours }))

        if (validAssessments.isNotEmpty()) {
            val top = validAssessments.take(3)
            val lines = top.mapIndexed { idx, a ->
                val c = courseMap[a.courseId]?.code ?: "COURSE"
                val hrs = a.estimatedHours.toInt()
                val wt = a.weightPercent.toInt()
                "${idx + 1}. $c ${a.title} due ${a.dueDate} (${wt}%, ~${hrs}h load)"
            }
            val first = top.first()
            val firstCourse = courseMap[first.courseId]?.code ?: "COURSE"
            return "Start with $firstCourse ${first.title} (${first.weightPercent.toInt()}%, due ${first.dueDate}, ~${first.estimatedHours.toInt()}h load). Ranked priority:\n" +
                lines.joinToString("\n")
        }

        val totalLoad = loadResult?.totalSemesterHours ?: 0.0
        val peakWeek = loadResult?.weeks?.maxByOrNull { it.totalHours }
        val peakHours = peakWeek?.totalHours ?: 0.0
        val peakNum = peakWeek?.weekNumber ?: 1
        return "Your semester has ${assessments.size} assessments totaling ${totalLoad.toInt()} hours. Peak load occurs in week $peakNum at ${peakHours.toInt()} hours against your ${semester.capacityHoursPerWeek.toInt()} capacity."
    }
}
