package com.example.expressora.dashboard.user.learn

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.request.ImageRequest
import coil.decode.GifDecoder
import com.example.expressora.R
import com.example.expressora.components.top_nav3.TopNav3
import com.example.expressora.components.user_bottom_nav.BottomNav
import com.example.expressora.dashboard.user.community_space.CommunitySpaceActivity
import com.example.expressora.dashboard.user.quiz.QuizActivity
import com.example.expressora.dashboard.user.translation.TranslationActivity
import com.example.expressora.ui.theme.InterFontFamily
import com.example.expressora.backend.LessonRepository
import com.example.expressora.dashboard.admin.learningmanagement.Lesson
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast
import android.media.MediaPlayer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow

fun getMimeType(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.getType(uri)
    } catch (e: Exception) {
        null
    }
}

fun getVideoFrame(context: Context, uri: Uri): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val bitmap = retriever.getFrameAtTime(1_000_000)
        retriever.release()
        bitmap
    } catch (e: Exception) {
        null
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1) name = it.getString(index)
        }
    }
    // Fallback: try to extract filename from URI path
    if (name.isNullOrEmpty()) {
        val uriString = uri.toString()
        val lastSegment = uriString.substringAfterLast("/")
        if (lastSegment.isNotEmpty() && lastSegment != uriString) {
            name = lastSegment.substringBefore("?") // Remove query parameters if any
        }
    }
    return name
}

fun getGifDataUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val bytes = stream.readBytes()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            "data:image/gif;base64,$base64"
        }
    } catch (e: Exception) {
        null
    }
}

class LearnActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LearnApp() }
    }
}

@Composable
fun LearnApp() {
    val navController = rememberNavController()
    val completedLessons = remember { mutableStateListOf<String>() }
    val context = LocalContext.current
    val lessonRepository = remember { LessonRepository() }
    val allLessons = remember { mutableStateListOf<Lesson>() }
    val scope = rememberCoroutineScope()
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }

    var currentScreen by remember { mutableStateOf("lessonList") }

    // Function to refresh lessons from Firestore
    fun refreshLessons() {
        scope.launch(Dispatchers.IO) {
            val result = lessonRepository.getLessons()
            result.onSuccess { lessons ->
                withContext(Dispatchers.Main) {
                    allLessons.clear()
                    // Sort by lastUpdated descending (most recent first)
                    val sortedLessons = lessons.sortedByDescending { it.lastUpdated }
                    allLessons.addAll(sortedLessons)
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    Log.e("LearnApp", "Failed to load lessons: ${e.message}", e)
                }
            }
        }
    }

    // Load lessons from Firestore on init and when screen becomes visible
    LaunchedEffect(Unit) {
        refreshLessons()
    }
    
    // Refresh when navigating back to lesson list
    LaunchedEffect(currentScreen) {
        if (currentScreen == "lessonList") {
            refreshLessons()
        }
    }

    Scaffold(
        topBar = {
            if (currentScreen != "detection") {
                TopNav3(onTranslateClick = {
                    context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
                })
            }
        }, bottomBar = {
            BottomNav(
                onLearnClick = { /* Already in learn */ },
                onCameraClick = {
                    context.startActivity(
                        Intent(
                            context, TranslationActivity::class.java
                        )
                    )
                },
                onQuizClick = { context.startActivity(Intent(context, QuizActivity::class.java)) })
        }, containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = "lessonList") {
                composable("lessonList") {
                    currentScreen = "lessonList"
                    LessonListScreen(
                        lessons = allLessons,
                        completedLessons = completedLessons, 
                        onLessonSelected = { lesson ->
                            selectedLesson = lesson
                            navController.navigate("lessonDetail")
                        })
                }

                composable("lessonDetail") {
                    currentScreen = "lessonDetail"
                    selectedLesson?.let { lesson ->
                        LessonDetailScreen(
                            lesson = lesson,
                            onTryItOut = {
                                navController.navigate("detection")
                            })
                    } ?: run {
                        // If no lesson selected, go back to list
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }


                composable("detection") {
                    currentScreen = "detection"
                    selectedLesson?.let { lesson ->
                        // Mark lesson as completed when entering Try It Out
                        if (!completedLessons.contains(lesson.title)) {
                            completedLessons.add(lesson.title)
                        }
                        DetectionScreen(
                            tryItems = lesson.tryItems,
                            onDetectionFinished = { 
                                // Clean up camera immediately before navigation for smooth transition
                                navController.navigate("completion")
                            },
                            onBack = {
                                // Clean up camera immediately before navigation for smooth transition
                                navController.popBackStack()
                            }
                        )
                    } ?: run {
                        // If no lesson selected, go back
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }

                composable("completion") {
                    currentScreen = "completion"
                    LessonCompletionScreen(
                        onNextCourse = {
                            navController.popBackStack(
                                "lessonList", inclusive = false
                            )
                        })
                }
            }
        }
    }
}

@Composable
fun LessonListScreen(
    lessons: List<Lesson>,
    completedLessons: List<String>, 
    onLessonSelected: (Lesson) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        Text(
            text = "Learn",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(16.dp)
        )

        if (lessons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No lessons available yet.", fontFamily = InterFontFamily, color = Color(0xFF666666))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 8.dp, // Space between "Learn" title and first lesson
                    bottom = 24.dp // Space between last lesson and bottom nav
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(lessons, key = { it.id }) { lesson ->
                    val isCompleted = completedLessons.contains(lesson.title)
            LessonRow(
                        title = lesson.title,
                        subtitle = lesson.content,
                isCompleted = isCompleted,
                        onClick = { onLessonSelected(lesson) })
                }
            }
        }
    }
}

@Composable
fun LessonRow(
    title: String, subtitle: String, isCompleted: Boolean, onClick: () -> Unit
) {
    val backgroundColor = if (isCompleted) Color(0xFFBBFFA0) else Color(0xFFF8F8F8)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF666666),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(30.dp)
            )
        }
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}

@Composable
fun LessonDetailScreen(
    lesson: Lesson,
    onTryItOut: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Auto-open files when selected (no dialog)
    LaunchedEffect(selectedMediaUri, selectedMediaType) {
        val uri = selectedMediaUri
        val type = selectedMediaType ?: (uri?.let { getMimeType(context, it) } ?: "")
        
        if (uri != null && type.isNotEmpty() && 
            !type.startsWith("video") && 
            !type.startsWith("audio") && 
            !type.startsWith("image") && 
            !uri.toString().startsWith("data:image")) {
            // It's a file - open it immediately
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, type)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // Clear selection
                selectedMediaUri = null
                selectedMediaType = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                text = lesson.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = lesson.content,
                fontSize = 16.sp,
                fontFamily = InterFontFamily,
                color = Color(0xFF666666),
                textAlign = TextAlign.Justify,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (lesson.attachments.isNotEmpty()) {
                Text(
                    text = "Attachments",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val chunkedAttachments = lesson.attachments.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunkedAttachments.forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { uri ->
                                val uriString = uri.toString()
                                val mimeType = getMimeType(context, uri) ?: ""
                                
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(140.dp)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }) {
                                            selectedMediaUri = uri
                                            selectedMediaType = mimeType
                                        }) {
                                    when {
                                        uriString.startsWith("data:image") -> {
                                            // Base64 image - check if it's a GIF
                                            val isGifBase64 = uriString.contains("image/gif", ignoreCase = true)
                                            if (isGifBase64) {
                                                // Use WebView for base64 GIF animation
                                                AndroidView(
                                                    factory = { ctx ->
                                                        WebView(ctx).apply {
                                                            webViewClient = WebViewClient()
                                                            settings.javaScriptEnabled = true
                                                            settings.loadWithOverviewMode = true
                                                            settings.useWideViewPort = true
                                                            settings.domStorageEnabled = true
                                                            loadDataWithBaseURL("file:///android_asset/", "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'></head><body style='margin:0;padding:0;background:transparent;'><img src='$uriString' style='width:100%;height:100%;object-fit:cover;display:block;'/></body></html>", "text/html", "UTF-8", null)
                                                        }
                                                    },
                                        modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                // Regular base64 image
                                                val base64String = uriString.substringAfter(",")
                                                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.NO_WRAP)
                                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                                bitmap?.let {
                                                    Image(
                                                        bitmap = it.asImageBitmap(),
                                                        contentDescription = "Lesson Image",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        }
                                        mimeType.startsWith("image") -> {
                                            // Image file - use WebView for GIFs to ensure animation
                                            val isGif = mimeType.contains("gif", ignoreCase = true) || 
                                                       uri.toString().lowercase().endsWith(".gif")
                                            if (isGif) {
                                                // Use WebView for GIF animation (most reliable)
                                                val gifDataUri = remember(uri) { getGifDataUri(context, uri) }
                                                AndroidView(
                                                    factory = { ctx ->
                                                        WebView(ctx).apply {
                                                            webViewClient = WebViewClient()
                                                            settings.javaScriptEnabled = true // Enable JS for better GIF support
                                                            settings.loadWithOverviewMode = true
                                                            settings.useWideViewPort = true
                                                            settings.domStorageEnabled = true
                                                            settings.allowFileAccess = true
                                                            settings.allowContentAccess = true
                                                            if (gifDataUri != null) {
                                                                loadDataWithBaseURL("file:///android_asset/", "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'></head><body style='margin:0;padding:0;background:transparent;'><img src='$gifDataUri' style='width:100%;height:100%;object-fit:cover;display:block;'/></body></html>", "text/html", "UTF-8", null)
                                                            } else {
                                                                loadUrl(uri.toString())
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                // Regular images
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(uri)
                                                        .build(),
                                                    contentDescription = "Lesson Image",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        mimeType.startsWith("video") -> {
                                            // Video - show thumbnail with play icon and circular black background
                                            val thumb = getVideoFrame(context, uri)
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                if (thumb != null) {
                                                    Image(
                                                        bitmap = thumb.asImageBitmap(),
                                                        contentDescription = "Video Thumbnail",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.VideoLibrary,
                                                            contentDescription = "Video",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(48.dp)
                                                        )
                                                    }
                                                }
                                                // Play icon overlay with circular black background
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.3f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(56.dp)
                                                            .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.PlayArrow,
                                                            contentDescription = "Play Video",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        mimeType.startsWith("audio") -> {
                                            // Audio file - show music icon with file name
                                            val audioFileName = getFileName(context, uri) ?: "Audio"
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFFF2F4F7)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        Icons.Default.MusicNote,
                                                        contentDescription = "Audio File",
                                                        tint = Color(0xFF666666),
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        audioFileName,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF666666),
                                                        fontFamily = InterFontFamily,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            // Other file types - show file icon with file name
                                            val fileName = getFileName(context, uri) ?: "File"
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFFF2F4F7)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        Icons.Default.InsertDriveFile,
                                                        contentDescription = "File",
                                                        tint = Color(0xFF666666),
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        fileName,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF666666),
                                                        fontFamily = InterFontFamily,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Button(
                onClick = onTryItOut,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(150.dp)
                    .height(35.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFACC15),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFFFACC15),
                    disabledContentColor = Color.Black
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Try It Out",
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Video player dialog
        selectedMediaUri?.let { uri ->
            val type = selectedMediaType ?: getMimeType(context, uri) ?: ""
            when {
                type.startsWith("video") -> {
                    VideoPlayerDialog(videoUri = uri, onDismiss = { 
                        selectedMediaUri = null
                        selectedMediaType = null
                    })
                }
                type.startsWith("audio") -> {
                    AudioPlayerDialog(audioUri = uri, onDismiss = { 
                        selectedMediaUri = null
                        selectedMediaType = null
                    })
                }
                else -> {
                    // Image or file viewer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                            .clickable { 
                                selectedMediaUri = null
                                selectedMediaType = null
                            }, 
                        contentAlignment = Alignment.Center
                    ) {
                        val uriString = uri.toString()
                        when {
                            uriString.startsWith("data:image") -> {
                                // Check if it's a base64 GIF
                                val isGifBase64 = uriString.contains("image/gif", ignoreCase = true)
                                if (isGifBase64) {
                                    // Use WebView for base64 GIF animation
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                webViewClient = WebViewClient()
                                                settings.javaScriptEnabled = true
                                                settings.loadWithOverviewMode = true
                                                settings.useWideViewPort = true
                                                settings.domStorageEnabled = true
                                                loadDataWithBaseURL("file:///android_asset/", "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'></head><body style='margin:0;padding:0;background:transparent;display:flex;justify-content:center;align-items:center;height:100vh;'><img src='$uriString' style='max-width:100%;max-height:100%;object-fit:contain;display:block;'/></body></html>", "text/html", "UTF-8", null)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .fillMaxHeight(0.6f)
                                    )
                                } else {
                                    // Regular base64 image
                                    val base64String = uriString.substringAfter(",")
                                    val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.NO_WRAP)
                                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    bitmap?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                    contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .fillMaxHeight(0.6f),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                            type.startsWith("image") -> {
                                // Image viewer - use WebView for GIFs to ensure animation
                                val isGif = type.contains("gif", ignoreCase = true) || 
                                           uri.toString().lowercase().endsWith(".gif")
                                if (isGif) {
                                    // Use WebView for GIF animation (most reliable)
                                    val gifDataUri = remember(uri) { getGifDataUri(context, uri) }
                                    AndroidView(
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                webViewClient = WebViewClient()
                                                settings.javaScriptEnabled = true // Enable JS for better GIF support
                                                settings.loadWithOverviewMode = true
                                                settings.useWideViewPort = true
                                                settings.domStorageEnabled = true
                                                settings.allowFileAccess = true
                                                settings.allowContentAccess = true
                                                if (gifDataUri != null) {
                                                    loadDataWithBaseURL("file:///android_asset/", "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'></head><body style='margin:0;padding:0;background:transparent;display:flex;justify-content:center;align-items:center;height:100vh;'><img src='$gifDataUri' style='max-width:100%;max-height:100%;object-fit:contain;display:block;'/></body></html>", "text/html", "UTF-8", null)
                                                } else {
                                                    loadUrl(uri.toString())
                                                }
                                            }
                                        },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight(0.6f)
                )
                                } else {
                                    // Regular images
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(uri)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .fillMaxHeight(0.6f),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            else -> {
                                // File will be auto-opened by LaunchedEffect above
                                // Show nothing or a brief message
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetectionScreen(
    tryItems: List<String>,
    onDetectionFinished: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var showCheck by remember { mutableStateOf(false) }
    var autoFinishTriggered by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var currentItemIndex by remember { mutableStateOf(0) }
    var showInstruction by remember { mutableStateOf(true) }
    var isNavigatingBack by remember { mutableStateOf(false) }
    
    val currentItem = if (tryItems.isNotEmpty() && currentItemIndex < tryItems.size) {
        tryItems[currentItemIndex]
    } else {
        ""
    }

    val lifecycleOwner = context as ComponentActivity
    val previewView = remember { PreviewView(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Handle back button press - clean up camera first for smooth navigation
    BackHandler(onBack = {
        // Hide camera preview immediately
        isNavigatingBack = true
        // Clean up camera immediately before navigation for smooth transition (like Translation)
        try {
            cameraProvider?.unbindAll()
            cameraProvider = null
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
        // Navigate back immediately - no delay for instant transition to lesson details
        onBack()
    })

    // Clean up camera immediately when composable is disposed - improved for smooth navigation
    DisposableEffect(Unit) {
        onDispose {
            // Immediately unbind camera when screen is exited
            try {
                cameraProvider?.unbindAll()
                cameraProvider = null
            } catch (e: Exception) {
                // Ignore errors during cleanup
            }
        }
    }

    LaunchedEffect(useFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                val cameraPreview = androidx.camera.core.Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, cameraPreview)
                cameraProvider = provider
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(currentItemIndex) {
        showCheck = false
        if (tryItems.isNotEmpty() && currentItemIndex < tryItems.size) {
        delay(3000)
        showCheck = true
        delay(3000)
            if (currentItemIndex < tryItems.size - 1) {
                // Move to next item
                currentItemIndex++
            } else {
                // Last item completed, finish
        if (!autoFinishTriggered) {
            autoFinishTriggered = true
            onDetectionFinished()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNavigatingBack) Color.Transparent else Color.Black)
    ) {
        // Hide camera preview immediately when navigating back
        if (!isNavigatingBack) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })
        }

        // Hide all UI elements when navigating back
        if (!isNavigatingBack) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera }, modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Camera,
                            contentDescription = "Flip Camera",
                            tint = Color(0xFFFACC15)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Show instruction for 5-20 seconds then hide
                    if (showInstruction) {
                        Text(
                            text = "Put your hands in front of the camera",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // Hide instruction after 5 seconds
        LaunchedEffect(Unit) {
            delay(5000)
            showInstruction = false
        }

        // Hide card when navigating back
        if (!isNavigatingBack) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                text = "The prompt will move on to the next step once you perform the sign language correctly.",
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Justify
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (currentItem.isNotEmpty()) currentItem else "No items",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.Black,
                                    maxLines = 1
                                )
                                if (showCheck) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Confirm",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        
                        // Skip button at bottom right with circular ripple effect (same style as Try It Out button)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape) // Clip to circular shape so ripple is bounded
                                .background(Color(0xFFFACC15)) // Same yellow color as Try It Out button
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        if (currentItemIndex < tryItems.size - 1) {
                                            currentItemIndex++
                                            showCheck = false
                                        } else {
                                            onDetectionFinished()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowRight,
                                contentDescription = "Skip",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonCompletionScreen(onNextCourse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = R.drawable.check_circle,
            contentDescription = null,
            modifier = Modifier.size(34.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You’re doing great!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You’ve successfully completed this step.\nKeep up the good work — your progress is impressive.",
            fontSize = 14.sp,
            fontFamily = InterFontFamily,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNextCourse,
            modifier = Modifier
                .width(150.dp)
                .height(35.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFACC15),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFFFACC15),
                disabledContentColor = Color.Black
            ),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                "Next Course",
                color = Color.Black,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

@UnstableApi
@Composable
fun VideoPlayerDialog(videoUri: Uri, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var player: ExoPlayer? by remember { mutableStateOf(null) }

    DisposableEffect(videoUri) {
        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
        player = exoPlayer

        onDispose {
            exoPlayer.release()
            player = null
        }
    }

    var aspectRatio by remember { mutableStateOf(16f / 9f) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(videoUri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 16
            val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 9
            aspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
            hasError = false
        } catch (e: Exception) {
            hasError = true
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center
            ) {
                Text("Error loading video", color = Color.White)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .aspectRatio(aspectRatio)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            this.useController = true
                            this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            this.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    }, 
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun AudioPlayerDialog(audioUri: Uri, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }

    DisposableEffect(audioUri) {
        val player = MediaPlayer().apply {
            setDataSource(context, audioUri)
            prepare()
            duration = this@apply.duration
            isLooping = true // Loop continuously
            start() // Auto-play when opened
        }
        mediaPlayer = player
        isPlaying = true

        onDispose {
            player.release()
            mediaPlayer = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Music icon with circular background matching learn design
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color(0xFFFACC15).copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "Audio",
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFFACC15)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Audio file name
                Text(
                    getFileName(context, audioUri) ?: "Audio File",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status text
                Text(
                    if (isPlaying) "Playing" else "Paused",
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Play/Pause button matching learn design (no hover effect)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Color(0xFFFACC15),
                            shape = CircleShape
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            mediaPlayer?.let { mp ->
                                if (isPlaying) {
                                    mp.pause()
                                    isPlaying = false
                                } else {
                                    mp.start()
                                    isPlaying = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp),
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLearnApp() {
    LearnApp()
}

