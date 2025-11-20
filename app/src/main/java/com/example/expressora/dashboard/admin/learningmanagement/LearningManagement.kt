package com.example.expressora.dashboard.admin.learningmanagement

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.expressora.components.admin_bottom_nav.BottomNav2
import com.example.expressora.components.top_nav3.TopNav3
import com.example.expressora.dashboard.admin.analytics.AnalyticsDashboardActivity
import com.example.expressora.dashboard.admin.communityspacemanagement.CommunitySpaceManagementActivity
import com.example.expressora.dashboard.admin.quizmanagement.QuizManagementActivity
import com.example.expressora.ui.theme.InterFontFamily
import com.example.expressora.backend.LessonRepository
import com.example.expressora.backend.AILessonService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class Lesson(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var content: String = "",
    var attachments: List<Uri> = emptyList(),
    var tryItems: List<String> = emptyList(),
    var lastUpdated: Long = System.currentTimeMillis()
)

private val AppBackground = Color(0xFFF8F8F8)
private val CardSurface = Color.White
private val MutedText = Color(0xFF666666)
private val Accent = Color(0xFFFACC15)

fun getTimeAgo(time: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - time
    val minutes = diff / 60000
    val hours = diff / 3600000
    val days = diff / 86400000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "$days day${if (days > 1) "s" else ""} ago"
    }
}

fun formatDate(time: Long): String {
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return fmt.format(Date(time))
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
    return name
}

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

class LearningManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val customSelectionColors = TextSelectionColors(
                handleColor = Color(0xFFFACC15),
                backgroundColor = Color(0x33FACC15)
            )

            CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
                LessonApp()
            }
        }
    }
}

// Helper function to convert image to base64 data URI
suspend fun convertImageToBase64(context: Context, imageUri: Uri): Pair<String?, String?> {
    return try {
        val contentResolver: ContentResolver = context.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(imageUri)

        if (inputStream == null) {
            val errorMsg = "Failed to open input stream from URI: $imageUri"
            android.util.Log.e("LearningManagement", errorMsg)
            return Pair(null, errorMsg)
        }
        
        // Check image file size (max 5MB)
        val maxFileSize = 5 * 1024 * 1024 // 5MB in bytes
        val availableBytes = inputStream.available()
        if (availableBytes > maxFileSize) {
            inputStream.close()
            val errorMsg = "Image size exceeds 5MB limit. Please use a smaller image."
            android.util.Log.e("LearningManagement", errorMsg)
            return Pair(null, errorMsg)
        }

        inputStream.use { stream ->
            // For images, use bitmap conversion
            val bitmap = BitmapFactory.decodeStream(stream)

            if (bitmap == null) {
                val errorMsg = "Failed to decode image"
                android.util.Log.e("LearningManagement", errorMsg)
                return Pair(null, errorMsg)
            }

            // Resize image to reduce size (max 800x800)
            val maxSize = 800
            val width = bitmap.width
            val height = bitmap.height
            val scale = if (width > height) {
                maxSize.toFloat() / width
            } else {
                maxSize.toFloat() / height
            }

            val resizedBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            // Compress to JPEG (quality 70%)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val imageBytes = outputStream.toByteArray()

            // Convert to base64
            val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64String"

            Pair(dataUri, null)
        }
    } catch (e: Exception) {
        val errorMsg = "Error converting image: ${e.message ?: e.javaClass.simpleName}"
        android.util.Log.e("LearningManagement", errorMsg, e)
        Pair(null, errorMsg)
    }
}

@Composable
fun LessonApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val lessonRepository = remember { LessonRepository() }
    val sharedPref = remember { context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE) }
    val adminEmail = remember { sharedPref.getString("user_email", "") ?: "" }
    val scope = rememberCoroutineScope()

    val allLessons = remember { mutableStateListOf<Lesson>() }
    var isLoading by remember { mutableStateOf(true) }

    // Function to refresh lessons from Firestore
    fun refreshLessons() {
        scope.launch(Dispatchers.IO) {
            val result = lessonRepository.getLessons()
            result.onSuccess { lessons ->
                withContext(Dispatchers.Main) {
                    allLessons.clear()
                    allLessons.addAll(lessons)
                    isLoading = false
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Failed to load lessons: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Load lessons from Firestore on init
    LaunchedEffect(Unit) {
        refreshLessons()
    }

    Scaffold(
        topBar = {
            TopNav3(onTranslateClick = {
                context.startActivity(Intent(context, CommunitySpaceManagementActivity::class.java))
            })
        }, bottomBar = {
            BottomNav2(onLearnClick = { /* already in learning management */ }, onAnalyticsClick = {
                context.startActivity(Intent(context, AnalyticsDashboardActivity::class.java))
            }, onQuizClick = {
                context.startActivity(Intent(context, QuizManagementActivity::class.java))
            })
        }, containerColor = AppBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .background(AppBackground)
                .fillMaxSize()
        ) {
            NavHost(navController = navController, startDestination = "list") {

                composable("list") {
                    val listContext = LocalContext.current
                    val listScope = rememberCoroutineScope()
                    
                    // Refresh lessons when navigating to list screen
                    LaunchedEffect(Unit) {
                        listScope.launch(Dispatchers.IO) {
                            val result = lessonRepository.getLessons()
                            result.onSuccess { lessons ->
                                withContext(Dispatchers.Main) {
                                    allLessons.clear()
                                    allLessons.addAll(lessons)
                                }
                            }
                        }
                    }
                    
                    LessonListScreen(
                        lessons = allLessons,
                        onAddLesson = { navController.navigate("add") },
                        onEditLesson = { lessonId -> navController.navigate("edit/$lessonId") },
                        onDeleteLesson = { lessonId ->
                            listScope.launch(Dispatchers.IO) {
                                val result = lessonRepository.deleteLesson(lessonId)
                                withContext(Dispatchers.Main) {
                                    result.onSuccess {
                                        // Update UI immediately
                                        val idx = allLessons.indexOfFirst { it.id == lessonId }
                                        if (idx != -1) {
                                            allLessons.removeAt(idx)
                                        }
                                        Toast.makeText(listContext, "Lesson deleted", Toast.LENGTH_SHORT).show()
                                        // Refresh from Firestore to ensure sync
                                        scope.launch(Dispatchers.IO) {
                                            val refreshResult = lessonRepository.getLessons()
                                            refreshResult.onSuccess { lessons ->
                                                withContext(Dispatchers.Main) {
                                                    allLessons.clear()
                                                    allLessons.addAll(lessons)
                                                    // Hide loading indicator after refresh completes
                                                    // Loading is managed in LessonListScreen, we need to trigger recomposition
                                                }
                                            }.onFailure {
                                                withContext(Dispatchers.Main) {
                                                    // Hide loading indicator even on refresh failure
                                                }
                                            }
                                        }
                                    }.onFailure { e ->
                                        Toast.makeText(listContext, "Failed to delete lesson: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                }

                composable("add") {
                    val addContext = LocalContext.current
                    val addScope = rememberCoroutineScope()
                    val temp = remember { Lesson() }
                    LessonEditorScreen(
                        title = "Add Lesson",
                        lesson = temp,
                        isEditMode = false,
                        onSaveConfirmed = { saved ->
                            // Convert image attachments to base64
                            addScope.launch(Dispatchers.IO) {
                                val processedAttachments = mutableListOf<Uri>()
                                saved.attachments.forEach { uri ->
                                    val mimeType = getMimeType(addContext, uri) ?: ""
                                    if (mimeType.startsWith("image/")) {
                                        val (base64Uri, error) = convertImageToBase64(addContext, uri)
                                        if (base64Uri != null) {
                                            processedAttachments.add(Uri.parse(base64Uri))
                                        } else {
                                            // If conversion fails, keep original URI
                                            processedAttachments.add(uri)
                                        }
                                    } else {
                                        // For videos, audio, and other files, keep as URI
                                        processedAttachments.add(uri)
                                    }
                                }
                                
                                val lessonToSave = saved.copy(
                                    attachments = processedAttachments,
                                    lastUpdated = System.currentTimeMillis()
                                )
                                
                                val result = lessonRepository.saveLesson(lessonToSave, adminEmail, isNew = true)
                                withContext(Dispatchers.Main) {
                                    result.onSuccess { lessonId ->
                                        // Update UI immediately with new lesson
                                        val newLesson = lessonToSave.copy(id = lessonId)
                                        allLessons.add(0, newLesson)
                                        Toast.makeText(addContext, "Lesson added", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                        // Refresh from Firestore to ensure sync
                                        scope.launch(Dispatchers.IO) {
                                            val refreshResult = lessonRepository.getLessons()
                                            refreshResult.onSuccess { lessons ->
                                                withContext(Dispatchers.Main) {
                                                    allLessons.clear()
                                                    allLessons.addAll(lessons)
                                                }
                                            }
                                        }
                                    }.onFailure { e ->
                                        Toast.makeText(addContext, "Failed to save lesson: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                }

                composable("edit/{lessonId}") { backStackEntry ->
                    val editContext = LocalContext.current
                    val editScope = rememberCoroutineScope()
                    val lessonId = backStackEntry.arguments?.getString("lessonId") ?: return@composable
                    val idx = allLessons.indexOfFirst { it.id == lessonId }
                    if (idx == -1) return@composable
                    val lessonCopy = remember {
                        allLessons[idx].copy(
                            attachments = allLessons[idx].attachments.toList(),
                            tryItems = allLessons[idx].tryItems.toList()
                        )
                    }
                    LessonEditorScreen(
                        title = "Edit Lesson",
                        lesson = lessonCopy,
                        isEditMode = true,
                        onSaveConfirmed = { updated ->
                            // Convert image attachments to base64
                            editScope.launch(Dispatchers.IO) {
                                val processedAttachments = mutableListOf<Uri>()
                                updated.attachments.forEach { uri ->
                                    val uriString = uri.toString()
                                    // If already a base64 data URI, keep it
                                    if (uriString.startsWith("data:")) {
                                        processedAttachments.add(uri)
                                    } else {
                                        val mimeType = getMimeType(editContext, uri) ?: ""
                                        if (mimeType.startsWith("image/")) {
                                            val (base64Uri, error) = convertImageToBase64(editContext, uri)
                                            if (base64Uri != null) {
                                                processedAttachments.add(Uri.parse(base64Uri))
                                            } else {
                                                // If conversion fails, keep original URI
                                                processedAttachments.add(uri)
                                            }
                                        } else {
                                            // For videos, audio, and other files, keep as URI
                                            processedAttachments.add(uri)
                                        }
                                    }
                                }
                                
                                val lessonToSave = updated.copy(
                                    attachments = processedAttachments,
                                    lastUpdated = System.currentTimeMillis()
                                )
                                
                                val result = lessonRepository.saveLesson(lessonToSave, adminEmail, isNew = false)
                                withContext(Dispatchers.Main) {
                                    result.onSuccess {
                                        // Update UI immediately
                                        val i = allLessons.indexOfFirst { it.id == updated.id }
                                        if (i != -1) {
                                            allLessons[i] = lessonToSave
                                        }
                                        Toast.makeText(editContext, "Lesson updated", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                        // Refresh from Firestore to ensure sync
                                        scope.launch(Dispatchers.IO) {
                                            val refreshResult = lessonRepository.getLessons()
                                            refreshResult.onSuccess { lessons ->
                                                withContext(Dispatchers.Main) {
                                                    allLessons.clear()
                                                    allLessons.addAll(lessons)
                                                }
                                            }
                                        }
                                    }.onFailure { e ->
                                        Toast.makeText(editContext, "Failed to update lesson: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                }
            }
        }
    }
}

@Composable
fun LessonListScreen(
    lessons: SnapshotStateList<Lesson>,
    onAddLesson: () -> Unit,
    onEditLesson: (String) -> Unit,
    onDeleteLesson: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiLessonService = remember { AILessonService() }
    val lessonRepository = remember { LessonRepository() }
    val sharedPref = remember { context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE) }
    val adminEmail = remember { sharedPref.getString("user_email", "") ?: "" }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortExpanded by remember { mutableStateOf(false) }
    var sortSelection by rememberSaveable { mutableStateOf("Latest") }

    val deleteDialog = remember { mutableStateOf<Pair<Boolean, String?>>(false to null) }
    val isLoading = remember { mutableStateOf(false) }
    val allSortOptions = listOf("Latest", "Oldest")
    val sortOptions = remember { mutableStateListOf(*allSortOptions.toTypedArray()) }
    
    // AI Generation Dialog State
    val showAIGenerationDialog = remember { mutableStateOf(false) }
    val aiTopicInput = remember { mutableStateOf("") }
    val aiCountInput = remember { mutableStateOf("1") }
    val isGeneratingAI = remember { mutableStateOf(false) }
    
    // Hide loading indicator when lessons list changes (after delete completes)
    LaunchedEffect(lessons.size) {
        if (isLoading.value) {
            isLoading.value = false
        }
    }

    val filtered = lessons.filter { lesson ->
        val q = searchQuery.trim().lowercase()
        q.isEmpty() || lesson.title.lowercase().contains(q) || lesson.content.lowercase()
            .contains(q)
    }.sortedWith(compareBy { if (sortSelection == "Latest") -it.lastUpdated else it.lastUpdated })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Lessons",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.weight(1f))
            // AI Generate Lesson Button
            IconButton(onClick = { showAIGenerationDialog.value = true }) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI Generate Lesson",
                    tint = Accent
                )
            }
            // Manual Add Lesson Button
            IconButton(onClick = onAddLesson) {
                Icon(Icons.Default.Add, contentDescription = "Add Lesson")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Add a lesson to cover new topics",
            fontSize = 14.sp,
            color = MutedText,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search lessons", color = Color(0xFF666666)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search, contentDescription = "Search", tint = Color.Black
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White, RoundedCornerShape(50))
                    .shadow(2.dp, RoundedCornerShape(50)),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color(0xFF666666),
                    cursorColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            var dropdownWidth by remember { mutableStateOf(0) }
            Box(modifier = Modifier.onGloballyPositioned { coords ->
                dropdownWidth = coords.size.width
            }) {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { sortExpanded = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(sortSelection, color = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.ArrowDropDown, contentDescription = "Sort", tint = Color.Black
                    )
                }

                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false },
                    modifier = Modifier
                        .width(with(LocalDensity.current) { dropdownWidth.toDp() })
                        .background(Color.White)
                ) {
                    sortOptions.filter { it != sortSelection }.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt, color = Color.Black) }, onClick = {
                            sortSelection = opt
                            sortExpanded = false
                        })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "${filtered.size} lesson(s)",
            color = MutedText,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        val listState = rememberLazyListState()

        LaunchedEffect(sortSelection) {
            listState.animateScrollToItem(0)
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {
            if (isLoading.value) {
                // Show loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = listState
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (searchQuery.isBlank()) "No lessons available yet." else "No lessons found.",
                                    textAlign = TextAlign.Center,
                                    color = MutedText,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { lesson ->
                            LessonCard(
                                lesson = lesson,
                                onEdit = { onEditLesson(lesson.id) },
                                onDelete = { deleteDialog.value = true to lesson.id })
                        }
                    }
                }
            }
        }
    }


    // AI Generation Dialog
    if (showAIGenerationDialog.value) {
        Dialog(onDismissRequest = { 
            if (!isGeneratingAI.value) {
                showAIGenerationDialog.value = false 
            }
        }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI Generate Lesson",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Generate learning lessons automatically using AI. Enter a topic STRICTLY related to American Sign Language (ASL) and/or Filipino Sign Language (FSL).\n\nExamples: \"Basic ASL Greetings\", \"FSL Numbers 1-10\", \"ASL vs FSL Differences\", \"ASL Family Signs\", \"FSL Daily Conversation\", \"ASL Alphabet\", etc.\n\nThe AI will create comprehensive lesson content including title, detailed content, and practical \"Try It Out\" exercises - all focused on sign language learning.\n\nNote: You can add attachments (images, videos) to the generated lesson later.",
                        color = MutedText,
                        textAlign = TextAlign.Justify,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = aiTopicInput.value,
                        onValueChange = { aiTopicInput.value = it },
                        label = {
                            Text("Lesson Topic (ASL/FSL only)", fontFamily = InterFontFamily, color = Color(0xFF666666))
                        },
                        enabled = !isGeneratingAI.value,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Basic ASL Greetings, FSL Numbers, ASL vs FSL Differences") },
                        colors = OutlinedTextFieldDefaults.colors(
                            cursorColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color(0xFF666666),
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF666666)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = aiCountInput.value,
                        onValueChange = { newValue ->
                            val num = newValue.toIntOrNull()
                            if (num != null && num > 0 && num <= 5) {
                                aiCountInput.value = newValue
                            } else if (newValue.isEmpty()) {
                                aiCountInput.value = ""
                            }
                        },
                        label = {
                            Text("Number of Lessons (1-5)", fontFamily = InterFontFamily, color = Color(0xFF666666))
                        },
                        enabled = !isGeneratingAI.value,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            cursorColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color(0xFF666666),
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF666666)
                        ),
                        supportingText = {
                            Text(
                                "Maximum 5 lessons can be generated",
                                fontFamily = InterFontFamily,
                                color = Color(0xFF666666)
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isGeneratingAI.value) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Accent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Generating lesson(s)...",
                                fontFamily = InterFontFamily,
                                color = MutedText
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showAIGenerationDialog.value = false },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                            ) { 
                                Text("Cancel", color = Color(0xFF666666)) 
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val topic = aiTopicInput.value.trim()
                                    val count = aiCountInput.value.toIntOrNull() ?: 1
                                    
                                    if (topic.isEmpty()) {
                                        Toast.makeText(context, "Please enter a topic", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    if (count <= 0 || count > 5) {
                                        Toast.makeText(context, "Please enter a number between 1 and 5", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    isGeneratingAI.value = true // Loading sa dialog lang muna
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val result = aiLessonService.generateLesson(topic, count)
                                            
                                            withContext(Dispatchers.Main) {
                                                isGeneratingAI.value = false
                                                showAIGenerationDialog.value = false
                                                
                                                result.onSuccess { aiResponse ->
                                                    if (aiResponse.success && aiResponse.lessons != null) {
                                                        // Save all lessons sequentially
                                                        scope.launch(Dispatchers.IO) {
                                                            val savedLessons = mutableListOf<Lesson>()
                                                            for (generatedLesson in aiResponse.lessons) {
                                                                val newLesson = Lesson(
                                                                    title = generatedLesson.title,
                                                                    content = generatedLesson.content,
                                                                    tryItems = generatedLesson.tryItems,
                                                                    lastUpdated = System.currentTimeMillis()
                                                                )
                                                                
                                                                // Save to Firebase
                                                                val saveResult = lessonRepository.saveLesson(newLesson, adminEmail, isNew = true)
                                                                saveResult.onSuccess { lessonId ->
                                                                    savedLessons.add(newLesson.copy(id = lessonId))
                                                                }
                                                            }
                                                            
                                                            // After saving, show loading sa main list para mag-refresh
                                                            withContext(Dispatchers.Main) {
                                                                isLoading.value = true // Loading sa main list - saka lang after saving
                                                            }
                                                            
                                                            // Refresh lessons from Firestore to get updated list
                                                            val refreshResult = lessonRepository.getLessons()
                                                            refreshResult.onSuccess { refreshedLessons ->
                                                                withContext(Dispatchers.Main) {
                                                                    lessons.clear()
                                                                    lessons.addAll(refreshedLessons)
                                                                    isLoading.value = false
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Successfully generated and added ${savedLessons.size} lesson(s)!",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }.onFailure {
                                                                withContext(Dispatchers.Main) {
                                                                    // Still add saved lessons even if refresh fails
                                                                    lessons.addAll(savedLessons)
                                                                    isLoading.value = false
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Successfully generated and added ${savedLessons.size} lesson(s)!",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            aiResponse.message ?: "Failed to generate lessons",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }.onFailure { e ->
                                                    Toast.makeText(
                                                        context,
                                                        "Error generating lessons: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isGeneratingAI.value = false
                                                showAIGenerationDialog.value = false
                                                Toast.makeText(
                                                    context,
                                                    "Error: ${e.localizedMessage}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.Black
                                )
                            ) { 
                                Text("Generate") 
                            }
                        }
                    }
                }
            }
        }
    }

    if (deleteDialog.value.first) {
        ConfirmStyledDialog(
            title = "Delete Lesson",
            message = "Are you sure you want to delete this lesson?",
            confirmText = "Delete",
            confirmColor = Color.Red,
            onDismiss = { deleteDialog.value = false to null },
            onConfirm = {
                val idToDelete = deleteDialog.value.second
                deleteDialog.value = false to null
                
                if (idToDelete != null) {
                    // Show loading indicator
                    isLoading.value = true
                    onDeleteLesson(idToDelete)
                }
            })
    }
}

@Composable
fun LessonCard(lesson: Lesson, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFDE58A)), contentAlignment = Alignment.Center
            ) {
                if (lesson.attachments.isNotEmpty()) {
                    val first = lesson.attachments.first()
                    val ctx = LocalContext.current
                    val type = getMimeType(ctx, first) ?: ""
                    when {
                        type.startsWith("image") -> AsyncImage(
                            model = ImageRequest.Builder(ctx).data(first).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        type.startsWith("video") -> VideoThumbnail(first)
                        else -> Icon(
                            Icons.Default.MenuBook, contentDescription = null, tint = Color.Black
                        )
                    }
                } else {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    lesson.title.ifBlank { "Untitled Lesson" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = InterFontFamily
                )
                Text(
                    "Updated ${getTimeAgo(lesson.lastUpdated)}\n${formatDate(lesson.lastUpdated)}",
                    color = MutedText,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily
                )
            }

            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun VideoThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bitmap = remember(uri) { getVideoFrame(ctx, uri) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun ConfirmStyledDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmTextColor: Color = Color.White
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(message, color = MutedText)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Cancel", color = Color(0xFF666666))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm, colors = ButtonDefaults.buttonColors(
                            containerColor = confirmColor, contentColor = confirmTextColor
                        )
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

@SuppressLint("RememberReturnType")
@Composable
fun LessonEditorScreen(
    title: String,
    lesson: Lesson,
    isEditMode: Boolean,
    onSaveConfirmed: (Lesson) -> Unit,
) {
    val context = LocalContext.current
    var lessonTitle by rememberSaveable { mutableStateOf(lesson.title) }
    var lessonContent by rememberSaveable { mutableStateOf(lesson.content) }
    val attachmentsState = remember { lesson.attachments.toMutableStateList() }
    var tryInput by rememberSaveable { mutableStateOf(lesson.tryItems.joinToString(", ")) }
    val saveDialogVisible = remember { mutableStateOf(false) }

    val multiplePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(), onResult = { uris ->
            if (uris.isNotEmpty()) {
                // Edge case: limit attachments to max 20
                val maxAttachments = 20
                val currentCount = attachmentsState.size
                val remainingSlots = maxAttachments - currentCount
                
                if (remainingSlots <= 0) {
                    Toast.makeText(context, "Maximum $maxAttachments attachments allowed", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
                
                val urisToAdd = uris.take(remainingSlots)
                urisToAdd.forEach { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {
                    }
                }
                attachmentsState.addAll(urisToAdd)
                
                if (uris.size > remainingSlots) {
                    Toast.makeText(context, "Added ${urisToAdd.size} files. Maximum $maxAttachments attachments reached.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Files selected: ${urisToAdd.size}", Toast.LENGTH_SHORT).show()
                }
            }
        })

    val isSaveEnabled by remember(lessonTitle, lessonContent, tryInput, attachmentsState) {
        derivedStateOf {
            // Parse try items and validate (like wrong options in quiz)
            val parsedTryItems = tryInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val hasValidTryItems = parsedTryItems.isNotEmpty() && parsedTryItems.size <= 5
            
            // Validate text lengths (like quiz management)
            val titleTrimmed = lessonTitle.trim()
            val contentTrimmed = lessonContent.trim()
            val maxTitleLength = 200
            val maxContentLength = 5000
            val isValidTitleLength = titleTrimmed.length <= maxTitleLength
            val isValidContentLength = contentTrimmed.length <= maxContentLength
            
            if (!isEditMode) {
                // For new lessons, require all fields filled (like quiz)
                isValidTitleLength && titleTrimmed.isNotEmpty() &&
                isValidContentLength && contentTrimmed.isNotEmpty() &&
                hasValidTryItems
            } else {
                // For edit mode: allow spaces - just check length limits and if any change detected
                // (exactly like quiz management edit mode)
                val originalTryItemsText = lesson.tryItems.joinToString(", ")
                val titleChanged = lessonTitle != lesson.title
                val contentChanged = lessonContent != lesson.content
                val tryChanged = tryInput != originalTryItemsText
                val attachmentsChanged = attachmentsState.toList() != lesson.attachments
                
                // Only validate length limits, allow spaces/empty in edit mode
                isValidTitleLength && isValidContentLength && 
                (titleChanged || contentChanged || tryChanged || attachmentsChanged)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { if (isSaveEnabled) saveDialogVisible.value = true },
                enabled = isSaveEnabled
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save Lesson",
                    tint = if (isSaveEnabled) Color.Black else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = lessonTitle,
            onValueChange = { 
                if (it.length <= 200) {
                    lessonTitle = it
                } else {
                    Toast.makeText(context, "Title cannot exceed 200 characters", Toast.LENGTH_SHORT).show()
                }
            },
            label = { Text("Lesson Title", fontFamily = InterFontFamily, color = MutedText) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                cursorColor = Color.Black,
                focusedTextColor = Color.Black,
                unfocusedTextColor = MutedText,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = MutedText
            ),
            supportingText = {
                Text(
                    "${lessonTitle.trim().length}/200 characters",
                    fontFamily = InterFontFamily,
                    color = if (lessonTitle.trim().length > 200) Color.Red else Color(0xFF666666)
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = lessonContent,
            onValueChange = { 
                if (it.length <= 5000) {
                    lessonContent = it
                } else {
                    Toast.makeText(context, "Content cannot exceed 5000 characters", Toast.LENGTH_SHORT).show()
                }
            },
            label = { Text("Lesson Content", fontFamily = InterFontFamily, color = MutedText) },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            maxLines = 10,
            colors = OutlinedTextFieldDefaults.colors(
                cursorColor = Color.Black,
                focusedTextColor = Color.Black,
                unfocusedTextColor = MutedText,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = MutedText
            ),
            supportingText = {
                Text(
                    "${lessonContent.trim().length}/5000 characters",
                    fontFamily = InterFontFamily,
                    color = if (lessonContent.trim().length > 5000) Color.Red else Color(0xFF666666)
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = tryInput,
            onValueChange = { newValue ->
                // Count current options (before change)
                val currentOptions = tryInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                // Count new options (after change)
                val newOptions = newValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                // Check for duplicate try items (case-insensitive)
                val duplicateOptions = newOptions.groupingBy { it.lowercase() }.eachCount().filter { it.value > 1 }
                if (duplicateOptions.isNotEmpty()) {
                    Toast.makeText(context, "Duplicate try items are not allowed", Toast.LENGTH_SHORT).show()
                    return@OutlinedTextField
                }
                
                // If already have 5 options, prevent adding more commas
                if (currentOptions.size >= 5) {
                    // Count commas in current and new value
                    val currentCommaCount = tryInput.count { it == ',' }
                    val newCommaCount = newValue.count { it == ',' }
                    
                    // If trying to add a comma when already at 5 options, prevent it
                    if (newCommaCount > currentCommaCount) {
                        Toast.makeText(context, "Maximum 5 try items allowed", Toast.LENGTH_SHORT).show()
                        return@OutlinedTextField
                    }
                }
                
                // Limit to 5 try items (exactly like quiz management wrong options)
                if (newOptions.size <= 5) {
                    tryInput = newValue
                } else {
                    Toast.makeText(context, "Maximum 5 try items allowed", Toast.LENGTH_SHORT).show()
                }
            },
            label = {
                Text(
                    "Try It Out (comma separated, max 5)", fontFamily = InterFontFamily, color = MutedText
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                cursorColor = Color.Black,
                focusedTextColor = Color.Black,
                unfocusedTextColor = MutedText,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = MutedText
            ),
            supportingText = {
                val optionCount = tryInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size
                Text(
                    "$optionCount/5 items",
                    fontFamily = InterFontFamily,
                    color = if (optionCount > 5) Color.Red else Color(0xFF666666)
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Attachments", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(attachmentsState.size + 1) { index ->
                if (index < attachmentsState.size) {
                    val uri = attachmentsState[index]
                    val type = getMimeType(context, uri) ?: ""
                    val fileName = getFileName(context, uri) ?: "File"

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .shadow(2.dp, RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F4F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            type.startsWith("image") -> {
                                val uriString = uri.toString()
                                val isBase64 = uriString.startsWith("data:image")
                                val imageData = if (isBase64) uriString else uri
                                val bitmap = remember(imageData) {
                                    if (isBase64) {
                                        try {
                                            val base64String = uriString.substringAfter(",")
                                            if (base64String.isNotEmpty()) {
                                                val imageBytes = Base64.decode(base64String, Base64.NO_WRAP)
                                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                            } else null
                                        } catch (e: Exception) {
                                            null
                                        }
                                    } else {
                                        try {
                                            val inputStream = context.contentResolver.openInputStream(uri)
                                            inputStream?.use { BitmapFactory.decodeStream(it) }
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                }
                                
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(imageData)
                                            .crossfade(true).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            
                            type.startsWith("video") -> {
                                val thumb = remember(uri) { getVideoFrame(context, uri) }
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .align(Alignment.Center)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .padding(12.dp)
                                    )
                                }
                            }
                            
                            type.startsWith("audio") -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = "Audio",
                                        tint = Color.Black,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fileName,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        color = Color.Black
                                    )
                                }
                            }
                            
                            else -> Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fileName,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black
                                )
                            }
                        }

                        IconButton(
                            onClick = { attachmentsState.remove(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F4F7))
                            .clickable {
                                multiplePickerLauncher.launch(
                                    arrayOf("image/*", "video/*", "*/*")
                                )
                            }, contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }

    if (saveDialogVisible.value) {
        ConfirmStyledDialog(
            title = "Save Lesson",
            message = "Do you want to save this lesson?",
            confirmText = "Save",
            confirmColor = Accent,
            onDismiss = { saveDialogVisible.value = false },
            onConfirm = {
                saveDialogVisible.value = false
                
                // Edge case handling: parse and validate try items (like quiz management)
                val parsedTry = tryInput.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct() // Remove duplicates
                    .take(5) // Limit to 5
                
                val titleTrimmed = lessonTitle.trim()
                val contentTrimmed = lessonContent.trim()
                
                // Validate only for new lessons (edit mode allows spaces)
                if (!isEditMode) {
                    if (titleTrimmed.isEmpty() || contentTrimmed.isEmpty()) {
                        Toast.makeText(context, "Title and content cannot be empty", Toast.LENGTH_SHORT).show()
                        return@ConfirmStyledDialog
                    }
                    
                    if (parsedTry.isEmpty()) {
                        Toast.makeText(context, "At least one try item is required", Toast.LENGTH_SHORT).show()
                        return@ConfirmStyledDialog
                    }
                } else {
                    // Edit mode: validate length limits only
                    if (titleTrimmed.length > 200) {
                        Toast.makeText(context, "Title cannot exceed 200 characters", Toast.LENGTH_SHORT).show()
                        return@ConfirmStyledDialog
                    }
                    
                    if (contentTrimmed.length > 5000) {
                        Toast.makeText(context, "Content cannot exceed 5000 characters", Toast.LENGTH_SHORT).show()
                        return@ConfirmStyledDialog
                    }
                }
                
                // Edge case: validate attachments count
                if (attachmentsState.size > 20) {
                    Toast.makeText(context, "Maximum 20 attachments allowed", Toast.LENGTH_SHORT).show()
                    return@ConfirmStyledDialog
                }
                
                // Edge case: validate try items count
                if (parsedTry.size > 5) {
                    Toast.makeText(context, "Maximum 5 try items allowed", Toast.LENGTH_SHORT).show()
                    return@ConfirmStyledDialog
                }
                
                onSaveConfirmed(
                    lesson.copy(
                        title = titleTrimmed,
                        content = contentTrimmed,
                        attachments = attachmentsState.toList(),
                        tryItems = parsedTry,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            },
            confirmTextColor = Color.Black
        )
    }
}
