package com.example.expressora.dashboard.admin.tutorialmonitoring

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class VideoPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val videoId = intent.getStringExtra("videoId") ?: ""
        val videoTitle = intent.getStringExtra("videoTitle") ?: "Video"
        
        setContent {
            VideoPlayerScreen(videoId = videoId, videoTitle = videoTitle)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Lock orientation to landscape for better video viewing
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    
    override fun onPause() {
        super.onPause()
        // Unlock orientation when leaving
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

@Composable
fun VideoPlayerScreen(videoId: String, videoTitle: String) {
    val context = LocalContext.current
    
    // Extract video ID if it's a full URL
    val cleanVideoId = remember(videoId) {
        val extracted = when {
            videoId.contains("youtube.com/watch?v=") -> {
                videoId.substringAfter("v=").substringBefore("&")
            }
            videoId.contains("youtu.be/") -> {
                videoId.substringAfter("youtu.be/").substringBefore("?")
            }
            videoId.contains("youtube.com/embed/") -> {
                videoId.substringAfter("embed/").substringBefore("?")
            }
            else -> videoId.trim()
        }
        extracted
    }
    
    // Directly open YouTube when screen loads
    LaunchedEffect(cleanVideoId) {
        android.util.Log.d("VideoPlayer", "Opening video in YouTube: $cleanVideoId")
        openVideoInYouTube(context, cleanVideoId)
        // Close this activity after opening YouTube
        (context as? ComponentActivity)?.finish()
    }
    
    // Show loading while opening
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    text = "Opening in YouTube...",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

// Function to open video in YouTube app or browser
fun openVideoInYouTube(context: android.content.Context, videoId: String) {
    try {
        // Try YouTube app first
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        if (appIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(appIntent)
            android.util.Log.d("VideoPlayer", "✅ Opened in YouTube app: $videoId")
        } else {
            // Fallback to CustomTabs (in-app browser) or regular browser
            try {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setToolbarColor(android.graphics.Color.BLACK)
                    .build()
                customTabsIntent.launchUrl(context, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                android.util.Log.d("VideoPlayer", "✅ Opened in CustomTabs: $videoId")
            } catch (e: Exception) {
                // Final fallback to regular browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                context.startActivity(browserIntent)
                android.util.Log.d("VideoPlayer", "✅ Opened in browser: $videoId")
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("VideoPlayer", "❌ Cannot open video: ${e.message}", e)
        Toast.makeText(context, "Cannot open video: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
