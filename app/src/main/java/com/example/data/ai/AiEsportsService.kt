package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiEsportsService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun consultStrategist(prompt: String, context: String = ""): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide a high-level simulated strategic response if no API key is provided
            return@withContext Result.success(getFallbackStrategy(prompt))
        }

        try {
            val systemInstruction = "You are the Golden Square Esports Strategist and Free Fire Tournament Analyst. Provide elite tactical analysis, custom room rule breakdowns, optimal drop zones (Bermuda, Purgatory, Nexterra), gun/skill loadouts, and scoring advice for competitive Free Fire teams."
            val fullPrompt = if (context.isNotBlank()) "Context: $context\n\nQuestion/Scenario: $prompt" else prompt

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemInstruction\n\n$fullPrompt")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "high")
                    })
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-4-argon:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                return@withContext Result.failure(Exception("Gemini API error (${response.code}): $errorBody"))
            }

            val responseString = response.body?.string().orEmpty()
            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text")
                        if (text.isNotBlank()) {
                            sb.append(text)
                        }
                    }
                    if (sb.isNotEmpty()) {
                        return@withContext Result.success(sb.toString())
                    }
                }
            }
            Result.success("Analysis completed, but no text output was generated.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFallbackStrategy(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("bermuda") -> """
                [GOLDEN SQUARE STRATEGIST - BERMUDA TACTICAL BREAKDOWN]
                
                * Preferred Drop Zones: Peak (High Risk/High Reward), Pochinok (Tier-1 Rotation Hub), Mars Electric (Safe High Loot).
                * Safe Zone Edge Play: Keep 2-and-2 split with sniper on Clock Tower roof holding south approach while rushers secure Bimasakti strip.
                * Loadout Meta: Groza / MP40 or M1887. Character setup: Chrono / Tatsuya for rapid re-peeks, Alok/K for sustained zone-healing.
                * Tournament Point Strategy: Prioritize 4 placement points minimum before initiating contested combat around the Factory ring.
            """.trimIndent()
            lower.contains("purgatory") -> """
                [GOLDEN SQUARE STRATEGIST - PURGATORY HIGH-GROUND META]
                
                * Key High-Grounds: Brasila central ridges and Moathouse bridges.
                * Tactical Warning: Bridge bottlenecks are death traps; always reserve at least 4 Gloo Walls and smoke canisters for river crossings.
                * Composition: 1 Long-Range Marksman (AWM/Woodpecker), 2 Heavy Assault, 1 Zone Healer.
            """.trimIndent()
            lower.contains("nexterra") -> """
                [GOLDEN SQUARE STRATEGIST - NEXTERRA ZERO-G ADVICE]
                
                * Grav Labs: Exploit vertical vectoring to leap over conventional Gloo Wall barriers.
                * Loot Route: Plazaria -> Deca Square -> Intellect Center.
                * Skill Recommendations: Homer to slow rushing enemy flankers in open high-tech plazas.
            """.trimIndent()
            else -> """
                [GOLDEN SQUARE COMPETITIVE ANALYSIS]
                
                1. Scrim Entry Preparation: Ensure all 4 roster members have verified UIDs and ping < 60ms.
                2. Room Slot Management: Check hosting rules 15 minutes before the match start time.
                3. Gold Points Maximization: Host weekly custom rooms (-50 GP) to build prestige, and claim your monthly team bonus (+1,000 GP) to maintain squad wallet health.
                4. Match Communication: Designate a single in-game leader (IGL) for target prioritization and zone entry calls.
            """.trimIndent()
        }
    }
}
