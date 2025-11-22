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

data class YouTubeVideo(
    val videoId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val channelTitle: String,
    val position: Int
)

data class YouTubePlaylist(
    val playlistId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val playlistUrl: String,
    val publishedAt: String,
    val channelTitle: String,
    val itemCount: Int
)

data class YouTubePlaylistResponse(
    val success: Boolean,
    val videos: List<YouTubeVideo>? = null,
    val playlistId: String? = null,
    val totalVideos: Int = 0,
    val message: String? = null
)

data class PlaylistConfig(
    val playlistId: String?,
    val channelId: String?,
    val organizationName: String?,
    val useChannelVideos: Boolean = false
)

class YouTubeService {
    // Default Channel Configuration - Kakamay Movement
    private val DEFAULT_CHANNEL_ID = "UCuzuc0P8L1fVIeeZrNLnkQA"
    private val DEFAULT_ORGANIZATION_NAME = "Kakamay Movement"
    
    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
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

    suspend fun getChannelVideos(channelId: String? = null, maxResults: Int = 50): Result<YouTubePlaylistResponse> = withContext(Dispatchers.IO) {
        try {
            // Use provided channelId or default to Kakamay Movement channel
            val finalChannelId = channelId ?: DEFAULT_CHANNEL_ID

            if (finalChannelId.isBlank()) {
                return@withContext Result.failure(Exception("No channel ID configured. Please configure a YouTube channel in the admin settings."))
            }

            // Fetch videos from channel using the new backend endpoint
            val url = "${getBaseUrl()}/youtube/videos?channelId=$finalChannelId&maxResults=$maxResults"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    val videosArray = jsonResponse.getJSONArray("videos")
                    val videos = mutableListOf<YouTubeVideo>()

                    for (i in 0 until videosArray.length()) {
                        val v = videosArray.getJSONObject(i)
                        videos.add(
                            YouTubeVideo(
                                videoId = v.getString("id"),
                                title = v.getString("title"),
                                description = v.optString("description", ""),
                                thumbnailUrl = v.optString("thumbnailUrl", ""),
                                publishedAt = v.optString("publishedAt", ""),
                                channelTitle = v.optString("channelTitle", ""),
                                position = i
                            )
                        )
                    }

                    Result.success(
                        YouTubePlaylistResponse(
                            success = true,
                            videos = videos,
                            playlistId = finalChannelId,
                            totalVideos = videos.size
                        )
                    )
                } else {
                    val message = jsonResponse.optString("message", "Failed to fetch videos")
                    Result.failure(Exception(message))
                }
            } else {
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optString("message", "Failed to fetch channel videos")
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChannelPlaylists(channelId: String? = null, maxResults: Int = 50): Result<List<YouTubePlaylist>> = withContext(Dispatchers.IO) {
        try {
            val finalChannelId = channelId ?: DEFAULT_CHANNEL_ID

            if (finalChannelId.isBlank()) {
                return@withContext Result.failure(Exception("No channel ID configured."))
            }

            val url = "${getBaseUrl()}/youtube/playlists?channelId=$finalChannelId&maxResults=$maxResults"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    val playlistsArray = jsonResponse.getJSONArray("playlists")
                    val playlists = mutableListOf<YouTubePlaylist>()

                    for (i in 0 until playlistsArray.length()) {
                        val p = playlistsArray.getJSONObject(i)
                        playlists.add(
                            YouTubePlaylist(
                                playlistId = p.getString("id"),
                                title = p.getString("title"),
                                description = p.optString("description", ""),
                                thumbnailUrl = p.optString("thumbnailUrl", ""),
                                playlistUrl = p.getString("playlistUrl"),
                                publishedAt = p.optString("publishedAt", ""),
                                channelTitle = p.optString("channelTitle", ""),
                                itemCount = p.optInt("itemCount", 0)
                            )
                        )
                    }

                    Result.success(playlists)
                } else {
                    val message = jsonResponse.optString("message", "Failed to fetch playlists")
                    Result.failure(Exception(message))
                }
            } else {
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optString("message", "Failed to fetch playlists")
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylistVideos(playlistId: String, maxResults: Int = 50): Result<List<YouTubeVideo>> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/youtube/playlist-videos?playlistId=$playlistId&maxResults=$maxResults"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    val videosArray = jsonResponse.getJSONArray("videos")
                    val videos = mutableListOf<YouTubeVideo>()

                    for (i in 0 until videosArray.length()) {
                        val v = videosArray.getJSONObject(i)
                        videos.add(
                            YouTubeVideo(
                                videoId = v.getString("id"),
                                title = v.getString("title"),
                                description = v.optString("description", ""),
                                thumbnailUrl = v.optString("thumbnailUrl", ""),
                                publishedAt = v.optString("publishedAt", ""),
                                channelTitle = v.optString("channelTitle", ""),
                                position = i
                            )
                        )
                    }

                    Result.success(videos)
                } else {
                    val message = jsonResponse.optString("message", "Failed to fetch videos")
                    Result.failure(Exception(message))
                }
            } else {
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optString("message", "Failed to fetch playlist videos")
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylistVideos(playlistId: String? = null): Result<YouTubePlaylistResponse> = withContext(Dispatchers.IO) {
        try {
            // First, get the configured playlist ID from backend if not provided
            val finalPlaylistId = playlistId ?: run {
                val configResult = getPlaylistConfig()
                configResult.getOrNull()?.playlistId
            }

            if (finalPlaylistId == null) {
                return@withContext Result.failure(Exception("No playlist ID configured. Please configure a YouTube playlist in the admin settings."))
            }

            val url = "${getBaseUrl()}/youtube-playlist-videos?playlistId=$finalPlaylistId"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    val videosArray = jsonResponse.getJSONArray("videos")
                    val videos = mutableListOf<YouTubeVideo>()

                    for (i in 0 until videosArray.length()) {
                        val v = videosArray.getJSONObject(i)
                        videos.add(
                            YouTubeVideo(
                                videoId = v.getString("videoId"),
                                title = v.getString("title"),
                                description = v.getString("description"),
                                thumbnailUrl = v.getString("thumbnailUrl"),
                                publishedAt = v.getString("publishedAt"),
                                channelTitle = v.getString("channelTitle"),
                                position = v.optInt("position", i)
                            )
                        )
                    }

                    Result.success(
                        YouTubePlaylistResponse(
                            success = true,
                            videos = videos,
                            playlistId = jsonResponse.optString("playlistId", finalPlaylistId),
                            totalVideos = jsonResponse.optInt("totalVideos", videos.size)
                        )
                    )
                } else {
                    val message = jsonResponse.optString("message", "Failed to fetch videos")
                    Result.failure(Exception(message))
                }
            } else {
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optString("message", "Failed to fetch playlist videos")
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylistConfig(): Result<PlaylistConfig> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/youtube-playlist-config"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    Result.success(
                        PlaylistConfig(
                            playlistId = jsonResponse.optString("playlistId", null),
                            channelId = jsonResponse.optString("channelId", null),
                            organizationName = jsonResponse.optString("organizationName", null),
                            useChannelVideos = jsonResponse.optBoolean("useChannelVideos", false)
                        )
                    )
                } else {
                    val message = jsonResponse.optString("message", "Failed to fetch config")
                    Result.failure(Exception(message))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistConfig(
        playlistId: String? = null,
        channelId: String? = null,
        organizationName: String? = null,
        useChannelVideos: Boolean = false
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                if (playlistId != null) put("playlistId", playlistId)
                if (channelId != null) put("channelId", channelId)
                if (organizationName != null) put("organizationName", organizationName)
                put("useChannelVideos", useChannelVideos)
            }

            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("${getBaseUrl()}/youtube-playlist-config")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val success = jsonResponse.getBoolean("success")
                if (success) {
                    Result.success(true)
                } else {
                    val message = jsonResponse.optString("message", "Failed to update config")
                    Result.failure(Exception(message))
                }
            } else {
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optString("message", "Failed to update playlist config")
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

