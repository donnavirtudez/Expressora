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

data class GeneratedLesson(
    val title: String,
    val content: String,
    val tryItems: List<String>
)

data class AILessonResponse(
    val success: Boolean,
    val lessons: List<GeneratedLesson>? = null,
    val message: String? = null
)

class AILessonService {
    private val client = OkHttpClient.Builder()
        .callTimeout(180, TimeUnit.SECONDS) // 3 minutes for lesson generation (can generate up to 5 lessons)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
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
        val LOCAL_HOST_IP = "192.168.1.22" // Update this with your local IP
        return if (isEmulator()) "http://10.0.2.2:3000" else "http://$LOCAL_HOST_IP:3000"
    }

    suspend fun generateLesson(
        topic: String,
        count: Int = 1
    ): Result<AILessonResponse> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("topic", topic)
                put("count", count)
            }
            
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("${getBaseUrl()}/generate-lesson")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    val lessonsArray = jsonResponse.getJSONArray("lessons")
                    val lessons = mutableListOf<GeneratedLesson>()

                    for (i in 0 until lessonsArray.length()) {
                        val l = lessonsArray.getJSONObject(i)
                        val tryItemsArray = l.getJSONArray("tryItems")
                        val tryItems = mutableListOf<String>()
                        
                        for (j in 0 until tryItemsArray.length()) {
                            tryItems.add(tryItemsArray.getString(j))
                        }

                        lessons.add(
                            GeneratedLesson(
                                title = l.getString("title"),
                                content = l.getString("content"),
                                tryItems = tryItems
                            )
                        )
                    }

                    Result.success(AILessonResponse(success = true, lessons = lessons))
                } else {
                    val message = jsonResponse.optString("message", "Failed to generate lesson")
                    Result.success(AILessonResponse(success = false, message = message))
                }
            } else {
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optString("message", "Server error: ${response.code}")
                } catch (e: Exception) {
                    "Server error: ${response.code}"
                }
                Result.success(AILessonResponse(success = false, message = errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.success(AILessonResponse(
                success = false, 
                message = "Request timeout. Generating ${count} lesson(s) may take longer. Please try with fewer lessons (1-2) or try again."
            ))
        } catch (e: java.io.IOException) {
            if (e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("timed out", ignoreCase = true) == true) {
                Result.success(AILessonResponse(
                    success = false, 
                    message = "Request timeout. Generating ${count} lesson(s) took too long. Please try with fewer lessons (1-2) or try again."
                ))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

