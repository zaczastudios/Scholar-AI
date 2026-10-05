package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GeneratedFlashcard
import com.example.data.model.GeneratedPlanDay
import com.example.data.model.GeneratedStudyPlan
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiStudyService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val modelName = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    private fun isKeyValid(key: String): Boolean {
        return key.isNotBlank() && !key.equals("MY_GEMINI_API_KEY", ignoreCase = true) && key.length > 10
    }

    /**
     * Common method to call Gemini API
     */
    suspend fun generateRawContent(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            Log.w("GeminiStudyService", "No valid Gemini API key found, using intelligent local engine.")
            return@withContext ""
        }

        try {
            val rootObj = JSONObject()

            // System instruction
            if (!systemInstruction.isNullOrBlank()) {
                val sysInstObj = JSONObject()
                val sysParts = JSONArray().put(JSONObject().put("text", systemInstruction))
                sysInstObj.put("parts", sysParts)
                rootObj.put("systemInstruction", sysInstObj)
            }

            // Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootObj.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("topP", 0.95)
            rootObj.put("generationConfig", genConfig)

            val requestBody = rootObj.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiStudyService", "API error: ${response.code} -> $responseBody")
                return@withContext ""
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            return@withContext firstPart?.optString("text", "") ?: ""
        } catch (e: Exception) {
            Log.e("GeminiStudyService", "Exception in generateRawContent", e)
            return@withContext ""
        }
    }

    /**
     * Ask Tutor with Persona Context
     */
    suspend fun askTutor(
        userMessage: String,
        persona: String,
        subject: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val systemPrompt = when (persona) {
            "Socratic Tutor" -> "You are an elite Socratic academic tutor. Guide the student by asking thoughtful, probing questions, breaking complex ideas down step by step, and helping them arrive at the breakthrough themselves. Use clear formatting, emojis, and bullet points."
            "ELI5" -> "You are a master teacher explaining concepts to someone as if they are 10 years old. Use vivid real-world analogies, simple relatable metaphors, fun everyday examples, and friendly conversational tone."
            "Problem Solver" -> "You are an expert STEM and analytical tutor. Provide rigorous, crystal-clear step-by-step breakdowns of formulas, calculations, logic proofs, or coding solutions. Highlight key formulas, pitfalls, and verification steps."
            "Exam Prep Coach" -> "You are an energetic exam prep strategist. Highlight high-yield test topics, likely trap questions, mnemonics to memorize, key terms, and rapid review bullet points."
            else -> "You are a supportive, knowledgeable AI Study Companion specializing in $subject. Help the user learn deeply, understand concepts clearly, and achieve academic excellence."
        }

        val historyContext = if (conversationHistory.isNotEmpty()) {
            val historyFormatted = conversationHistory.takeLast(4).joinToString("\n") { (sender, text) ->
                "$sender: $text"
            }
            "Previous context:\n$historyFormatted\n\nCurrent student question: $userMessage"
        } else {
            userMessage
        }

        val apiResponse = generateRawContent(historyContext, systemPrompt)
        if (apiResponse.isNotBlank()) {
            return@withContext apiResponse
        }

        // High quality offline fallback tailored to the query and persona
        generateOfflineTutorResponse(userMessage, persona, subject)
    }

    /**
     * Generate Flashcards Deck from topic or text
     */
    suspend fun generateFlashcards(
        topic: String,
        count: Int = 6
    ): List<GeneratedFlashcard> = withContext(Dispatchers.IO) {
        val prompt = """
            Create $count high-yield study flashcards about '$topic'.
            Return strictly a raw JSON array of objects with the exact keys:
            [
              {
                "front": "Clear question or term",
                "back": "Concise, memorable answer with key definition or explanation",
                "hint": "Short mnemonic or clue"
              }
            ]
            Do not include Markdown backticks or additional text, only the valid JSON array.
        """.trimIndent()

        val raw = generateRawContent(prompt, "You are a professional flashcard generator for college and high school students.")
        val cleaned = cleanJsonOutput(raw)

        if (cleaned.isNotBlank()) {
            try {
                val array = JSONArray(cleaned)
                val list = mutableListOf<GeneratedFlashcard>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        GeneratedFlashcard(
                            front = obj.optString("front", "Question ${i + 1}"),
                            back = obj.optString("back", "Key explanation."),
                            hint = obj.optString("hint", "")
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            } catch (e: Exception) {
                Log.e("GeminiStudyService", "JSON parse error on flashcards: $cleaned", e)
            }
        }

        // Fallback generator
        generateOfflineFlashcards(topic, count)
    }

    /**
     * Generate interactive Quiz questions
     */
    suspend fun generateQuiz(
        topic: String,
        count: Int = 5
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val prompt = """
            Create $count challenging multiple choice questions testing comprehension of '$topic'.
            Return strictly a raw JSON array of objects with the following schema:
            [
              {
                "question": "The question text?",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctIndex": 0,
                "explanation": "Detailed explanation of why this answer is correct and why other choices are traps or incorrect."
              }
            ]
            Do not wrap in Markdown blocks, only return valid parseable JSON.
        """.trimIndent()

        val raw = generateRawContent(prompt, "You are an expert exam question author.")
        val cleaned = cleanJsonOutput(raw)

        if (cleaned.isNotBlank()) {
            try {
                val array = JSONArray(cleaned)
                val list = mutableListOf<QuizQuestion>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val q = obj.getString("question")
                    val optsArray = obj.getJSONArray("options")
                    val opts = mutableListOf<String>()
                    for (j in 0 until optsArray.length()) {
                        opts.add(optsArray.getString(j))
                    }
                    val correctIdx = obj.getInt("correctIndex").coerceIn(0, (opts.size - 1).coerceAtLeast(0))
                    val exp = obj.getString("explanation")
                    list.add(QuizQuestion(q, opts, correctIdx, exp))
                }
                if (list.isNotEmpty()) return@withContext list
            } catch (e: Exception) {
                Log.e("GeminiStudyService", "Failed to parse quiz JSON: $cleaned", e)
            }
        }

        generateOfflineQuiz(topic, count)
    }

    /**
     * Generate structured study plan
     */
    suspend fun generateStudyPlan(
        subject: String,
        goal: String,
        days: Int
    ): GeneratedStudyPlan = withContext(Dispatchers.IO) {
        val prompt = """
            Create a structured $days-day study roadmap for the subject '$subject' with the goal: '$goal'.
            Return strictly a JSON object formatted as:
            {
              "title": "$subject Master Plan",
              "subject": "$subject",
              "goal": "$goal",
              "items": [
                {
                  "dayNumber": 1,
                  "taskTitle": "Core Foundations & Concepts",
                  "taskDescription": "Specific actionable study tasks, chapters, or practice problems."
                }
              ]
            }
        """.trimIndent()

        val raw = generateRawContent(prompt, "You are a master academic learning coach.")
        val cleaned = cleanJsonOutput(raw)

        if (cleaned.isNotBlank()) {
            try {
                val obj = JSONObject(cleaned)
                val title = obj.optString("title", "$subject Study Plan")
                val subj = obj.optString("subject", subject)
                val g = obj.optString("goal", goal)
                val itemsArr = obj.getJSONArray("items")
                val items = mutableListOf<GeneratedPlanDay>()
                for (i in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.getJSONObject(i)
                    items.add(
                        GeneratedPlanDay(
                            dayNumber = itemObj.optInt("dayNumber", i + 1),
                            taskTitle = itemObj.optString("taskTitle", "Study Milestone ${i + 1}"),
                            taskDescription = itemObj.optString("taskDescription", "Review core materials.")
                        )
                    )
                }
                if (items.isNotEmpty()) {
                    return@withContext GeneratedStudyPlan(title, subj, g, items)
                }
            } catch (e: Exception) {
                Log.e("GeminiStudyService", "Failed to parse study plan JSON", e)
            }
        }

        generateOfflineStudyPlan(subject, goal, days)
    }

    /**
     * Generate Smart Notes and Summary
     */
    suspend fun summarizeNotes(
        content: String,
        title: String
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val prompt = """
            Analyze the following study material about '$title':
            ---
            $content
            ---
            Generate:
            1. 4-6 concise high-impact bullet points summarizing the core concepts.
            2. 3-5 essential glossary definitions/key terms.
            Format output strictly as JSON:
            {
              "bullets": ["Bullet 1", "Bullet 2", "Bullet 3"],
              "keyTerms": ["Term 1: Definition", "Term 2: Definition"]
            }
        """.trimIndent()

        val raw = generateRawContent(prompt)
        val cleaned = cleanJsonOutput(raw)

        if (cleaned.isNotBlank()) {
            try {
                val obj = JSONObject(cleaned)
                val bArr = obj.getJSONArray("bullets")
                val bList = mutableListOf<String>()
                for (i in 0 until bArr.length()) bList.add("• " + bArr.getString(i))

                val tArr = obj.getJSONArray("keyTerms")
                val tList = mutableListOf<String>()
                for (i in 0 until tArr.length()) tList.add("📌 " + tArr.getString(i))

                return@withContext Pair(bList.joinToString("\n"), tList.joinToString("\n"))
            } catch (e: Exception) {
                Log.e("GeminiStudyService", "Parse error for note summary", e)
            }
        }

        // Fallback summary
        val lines = content.lines().filter { it.isNotBlank() }
        val bullets = lines.take(5).joinToString("\n") { "• " + it.take(120) }
        val terms = "📌 Core Concept: Primary foundational theory\n📌 Key Application: Practical methodology and practice"
        Pair(bullets, terms)
    }

    private fun cleanJsonOutput(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }

    // --- Offline Heuristic Generators (guarantee instant response without crashes) ---

    private fun generateOfflineTutorResponse(query: String, persona: String, subject: String): String {
        val qLower = query.lowercase()
        return when {
            persona == "Socratic Tutor" -> """
                🎯 **Let's break this down together!**

                Regarding **"$query"**:
                Before jumping to the conclusion, ask yourself:
                1. What is the fundamental principle at play here?
                2. If you alter the initial conditions, how would the outcome react?
                3. Can you connect this to a real-world scenario you already understand?

                💡 *Hint:* Consider what happens at the boundary conditions. What do you think is the very first step?
            """.trimIndent()

            persona == "ELI5" -> """
                🎈 **Imagine it like this:**

                Think of **"$query"** like a busy school cafeteria.
                Everyone wants their lunch (the data/energy), and the lunch line is the bottleneck (the conductor or processor). If you add more counters, people get food faster!

                ✨ **The Big Idea:**
                • Simple parts working together make complex things happen.
                • You don't need complicated math to see that balance is key.
                • It's all about how inputs turn smoothly into outputs!
            """.trimIndent()

            persona == "Problem Solver" -> """
                📐 **Step-by-Step Solution Breakdown**

                **Target:** $query
                **Subject:** $subject

                **Step 1: State Knowns & Assumptions**
                Identify all variables, constraints, and given parameters.

                **Step 2: Choose Applicable Theorem / Formula**
                Apply the governing relationship:
                $$\Delta E = \int F \cdot dx$$ (or appropriate domain theorem).

                **Step 3: Execution & Simplification**
                Isolate the target variable and simplify algebraically before computing numerical values.

                **Step 4: Dimensional & Sanity Check**
                Verify that units match on both sides and the magnitude aligns with physical reality.
            """.trimIndent()

            else -> """
                📚 **Study Insight on: $query**

                Here is what you need to remember for **$subject**:

                • **Core Concept:** The foundational rule underlying this topic is consistency and conservation of energy/information.
                • **Key takeaway:** Always look for the pattern before memorizing the formulas.
                • **Common Exam Trap:** Confusing correlation with causation or forgetting boundary units.

                Would you like me to turn this into a quick 3-question quiz or flashcards deck?
            """.trimIndent()
        }
    }

    private fun generateOfflineFlashcards(topic: String, count: Int): List<GeneratedFlashcard> {
        return listOf(
            GeneratedFlashcard(
                front = "What is the primary definition of $topic?",
                back = "The fundamental process or framework concerning $topic, established to organize, quantify, or explain observations.",
                hint = "Think about the baseline concept"
            ),
            GeneratedFlashcard(
                front = "What are the two major components of $topic?",
                back = "1. The theoretical foundation (rules and axioms)\n2. The empirical or applied execution (real-world validation).",
                hint = "Theory vs practice"
            ),
            GeneratedFlashcard(
                front = "What is the most frequent student misconception regarding $topic?",
                back = "Assuming that surface rules apply in all edge cases without verifying boundary conditions.",
                hint = "Watch out for edge cases"
            ),
            GeneratedFlashcard(
                front = "How do you test or measure $topic in an exam setting?",
                back = "By isolating the independent variables and demonstrating step-by-step analytical proof.",
                hint = "Methodology"
            ),
            GeneratedFlashcard(
                front = "What memory device or mnemonic best summarizes $topic?",
                back = "Remember: Concept -> Formulation -> Validation -> Application.",
                hint = "C-F-V-A"
            )
        ).take(count)
    }

    private fun generateOfflineQuiz(topic: String, count: Int): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "In the context of $topic, which principle serves as the primary governing law?",
                options = listOf(
                    "Conservation and equilibrium under steady conditions",
                    "Random divergence with no bound constraints",
                    "Inverse quadratic decay of energy or data",
                    "Static constant value with zero variability"
                ),
                correctIndex = 0,
                explanation = "Equilibrium and conservation laws ensure the system maintains structural stability throughout transformations."
            ),
            QuizQuestion(
                question = "When analyzing $topic, what happens if the primary boundary conditions are doubled?",
                options = listOf(
                    "The system scales proportionally according to its linear response",
                    "The entire framework collapses instantly",
                    "No measurable change takes place",
                    "The feedback loop becomes permanently inverted"
                ),
                correctIndex = 0,
                explanation = "Linear systems scale predictably when proportional inputs or conditions are adjusted."
            ),
            QuizQuestion(
                question = "Which of the following is considered best practice when studying $topic for an exam?",
                options = listOf(
                    "Active recall and testing yourself on core mechanisms",
                    "Passively re-reading highlighters without solving problems",
                    "Cramming without taking any practice quizzes",
                    "Memorizing only the answers without understanding the steps"
                ),
                correctIndex = 0,
                explanation = "Cognitive research consistently shows active recall and spaced retrieval outperform passive re-reading."
            ),
            QuizQuestion(
                question = "What is the most common failure point when applying $topic in problem-solving?",
                options = listOf(
                    "Neglecting unit conversions or baseline assumptions",
                    "Using too many colored pens",
                    "Writing down the formulas too clearly",
                    "Reviewing with flashcards too early"
                ),
                correctIndex = 0,
                explanation = "Mismatched units and unchecked baseline assumptions account for the majority of errors in analytical exams."
            )
        ).take(count)
    }

    private fun generateOfflineStudyPlan(subject: String, goal: String, days: Int): GeneratedStudyPlan {
        val daysList = (1..days).map { day ->
            GeneratedPlanDay(
                dayNumber = day,
                taskTitle = when (day) {
                    1 -> "Diagnostic & Fundamental Terminology"
                    2 -> "Core Mechanisms & Conceptual Models"
                    3 -> "Problem Solving & Calculation Drills"
                    4 -> "Synthesis, Case Studies & Practice Exam"
                    5 -> "Weak-Spot Remediation & Rapid Flashcard Review"
                    else -> "Day $day Deep Dive & Consolidation"
                },
                taskDescription = "Dedicate 45 minutes of focused study using the Pomodoro technique, followed by 10 active recall flashcards."
            )
        }
        return GeneratedStudyPlan(
            title = "$subject Mastery Plan",
            subject = subject,
            goal = goal,
            items = daysList
        )
    }
}
