package com.example.expressora.backend

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeneratedQuestion(
    val question: String,
    val correctAnswer: String,
    val wrongOptions: List<String>
)

data class AIQuizResponse(
    val success: Boolean,
    val questions: List<GeneratedQuestion>? = null,
    val message: String? = null
)

class AIQuizService {
    private val client = OkHttpClient.Builder()
        .callTimeout(120, TimeUnit.SECONDS) // Longer timeout for AI generation (2 minutes for 10 questions)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("google/sdk_gphone") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT
    }

    private fun getBaseUrl(): String {
        val LOCAL_HOST_IP = "192.168.1.16" // Update this with your local IP
        return if (isEmulator()) "http://10.0.2.2:3000" else "http://$LOCAL_HOST_IP:3000"
    }

    suspend fun generateQuizQuestions(
        difficulty: String,
        count: Int = 5
    ): Result<AIQuizResponse> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("difficulty", difficulty)
                put("count", count)
            }
            
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("${getBaseUrl()}/generate-quiz-questions")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    val questionsArray = jsonResponse.getJSONArray("questions")
                    val questions = mutableListOf<GeneratedQuestion>()

                    for (i in 0 until questionsArray.length()) {
                        val q = questionsArray.getJSONObject(i)
                        val wrongOptionsArray = q.getJSONArray("wrongOptions")
                        val wrongOptions = mutableListOf<String>()
                        
                        for (j in 0 until wrongOptionsArray.length()) {
                            wrongOptions.add(wrongOptionsArray.getString(j))
                        }

                        questions.add(
                            GeneratedQuestion(
                                question = q.getString("question"),
                                correctAnswer = q.getString("correctAnswer"),
                                wrongOptions = wrongOptions
                            )
                        )
                    }

                    Result.success(AIQuizResponse(success = true, questions = questions))
                } else {
                    val message = jsonResponse.optString("message", "Failed to generate questions")
                    Result.success(AIQuizResponse(success = false, message = message))
                }
            } else {
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optString("message", "Server error: ${response.code}")
                } catch (e: Exception) {
                    "Server error: ${response.code}"
                }
                Result.success(AIQuizResponse(success = false, message = errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.success(AIQuizResponse(
                success = false, 
                message = "Request timeout. Generating ${count} questions may take longer. Please try with fewer questions (3-5) or try again."
            ))
        } catch (e: java.io.IOException) {
            if (e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("timed out", ignoreCase = true) == true) {
                Result.success(AIQuizResponse(
                    success = false, 
                    message = "Request timeout. Generating ${count} questions took too long. Please try with fewer questions (3-5) or try again."
                ))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

