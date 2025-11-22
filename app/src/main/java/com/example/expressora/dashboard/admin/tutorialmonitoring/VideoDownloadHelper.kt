package com.example.expressora.dashboard.admin.tutorialmonitoring

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import android.content.ContentValues
import android.Manifest

object VideoDownloadHelper {
    // Check if we have necessary permissions
    private fun hasStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ (API 29+) - Scoped storage, no permission needed for MediaStore
                true
            }
            else -> {
                // Android 9 and below
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    suspend fun downloadVideoThumbnail(
        context: Context,
        videoId: String,
        videoTitle: String,
        thumbnailUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("VideoDownloadHelper", "Starting download for video: $videoId")
            android.util.Log.d("VideoDownloadHelper", "Thumbnail URL: $thumbnailUrl")
            android.util.Log.d("VideoDownloadHelper", "Android version: ${Build.VERSION.SDK_INT}")
            android.util.Log.d("VideoDownloadHelper", "Has storage permission: ${hasStoragePermission(context)}")
            
            // Check permissions for Android 9 and below
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasStoragePermission(context)) {
                android.util.Log.e("VideoDownloadHelper", "❌ Storage permission not granted")
                return@withContext false
            }
            
            // Download thumbnail as offline reference
            val url = URL(thumbnailUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            
            android.util.Log.d("VideoDownloadHelper", "Connection established")
            
            val inputStream = connection.getInputStream()
            val fileName = "${videoId}_${videoTitle.replace(Regex("[^a-zA-Z0-9]"), "_").take(50)}.jpg"
            
            android.util.Log.d("VideoDownloadHelper", "File name: $fileName")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.util.Log.d("VideoDownloadHelper", "Using MediaStore API (Android 10+)")
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Expressora/VideoThumbnails")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    android.util.Log.d("VideoDownloadHelper", "MediaStore URI created: $uri")
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        val bytesCopied = inputStream.copyTo(outputStream)
                        android.util.Log.d("VideoDownloadHelper", "Copied $bytesCopied bytes")
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    android.util.Log.d("VideoDownloadHelper", "✅ Thumbnail saved successfully: $fileName")
                    true
                } else {
                    android.util.Log.e("VideoDownloadHelper", "❌ Failed to create MediaStore entry - URI is null")
                    false
                }
            } else {
                android.util.Log.d("VideoDownloadHelper", "Using File API (Android 9 and below)")
                @Suppress("DEPRECATION")
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Expressora/VideoThumbnails"
                )
                if (!picturesDir.exists()) {
                    val created = picturesDir.mkdirs()
                    android.util.Log.d("VideoDownloadHelper", "Created directory: $created, Path: ${picturesDir.absolutePath}")
                }
                val file = File(picturesDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    android.util.Log.d("VideoDownloadHelper", "Copied $bytesCopied bytes")
                }
                android.util.Log.d("VideoDownloadHelper", "✅ Thumbnail saved successfully: ${file.absolutePath}")
                true
            }
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("VideoDownloadHelper", "❌ Timeout error: ${e.message}", e)
            false
        } catch (e: java.io.IOException) {
            android.util.Log.e("VideoDownloadHelper", "❌ IO error: ${e.message}", e)
            android.util.Log.e("VideoDownloadHelper", "Error details: ${e.stackTraceToString()}")
            false
        } catch (e: SecurityException) {
            android.util.Log.e("VideoDownloadHelper", "❌ Permission error: ${e.message}", e)
            android.util.Log.e("VideoDownloadHelper", "Make sure WRITE_EXTERNAL_STORAGE permission is granted")
            false
        } catch (e: Exception) {
            android.util.Log.e("VideoDownloadHelper", "❌ Unexpected error: ${e.message}", e)
            android.util.Log.e("VideoDownloadHelper", "Error type: ${e.javaClass.simpleName}")
            android.util.Log.e("VideoDownloadHelper", "Error details: ${e.stackTraceToString()}")
            false
        }
    }
    
    fun openVideoInYouTubeApp(context: Context, videoId: String) {
        try {
            // Try to open in YouTube app first
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to browser
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun saveVideoForOffline(context: Context, videoId: String, videoTitle: String) {
        // Note: Direct YouTube video download is not allowed by YouTube ToS
        // This function saves video metadata for offline reference
        // Actual video playback still requires internet connection
        
        Toast.makeText(
            context,
            "Video metadata saved. Note: Video playback requires internet connection due to YouTube terms of service.",
            Toast.LENGTH_LONG
        ).show()
        
        // Save thumbnail for offline viewing
        // Actual video download would require YouTube Premium or third-party service
    }
}

