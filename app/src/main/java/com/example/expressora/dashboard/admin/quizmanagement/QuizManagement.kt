package com.example.expressora.dashboard.admin.quizmanagement

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import com.example.expressora.utils.RoleValidationUtil
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.expressora.backend.AIQuizService
import com.example.expressora.backend.QuizRepository
import com.example.expressora.components.admin_bottom_nav.BottomNav2
import com.example.expressora.components.top_nav3.TopNav3
import com.example.expressora.dashboard.admin.analytics.AnalyticsDashboardActivity
import com.example.expressora.dashboard.admin.communityspacemanagement.CommunitySpaceManagementActivity
import com.example.expressora.dashboard.admin.learningmanagement.LearningManagementActivity
import com.example.expressora.ui.theme.InterFontFamily
import android.content.Context
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Base64
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class Difficulty { EASY, MEDIUM, DIFFICULT, PRO }

data class Question(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var imageUri: Uri? = null,
    var correctAnswer: String = "",
    var wrongOptions: MutableList<String> = mutableListOf()
)

data class Quiz(
    var id: String = UUID.randomUUID().toString(),
    var difficulty: Difficulty = Difficulty.EASY,
    val questions: MutableList<Question> = mutableStateListOf(),
    var lastUpdated: Long = 0L // 0 means not updated yet
)

fun formatDate(time: Long): String {
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return fmt.format(Date(time))
}

// Helper function to create colored placeholder image with letter based on difficulty level
fun createDifficultyPlaceholderImage(difficulty: Difficulty): String {
    // Get difficulty letter (same as QuizCard)
    val difficultyLetter = when (difficulty) {
        Difficulty.EASY -> "E"
        Difficulty.MEDIUM -> "M"
        Difficulty.DIFFICULT -> "D"
        Difficulty.PRO -> "P"
    }
    
    // Background color matching QuizCard (yellow shade)
    val bgColor = 0xFFFDE58A.toInt()
    
    // Create a 100x100 pixel bitmap
    val width = 100
    val height = 100
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Fill background with yellow color
    canvas.drawColor(bgColor)
    
    // Draw letter in black, bold
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 40f // Scaled for 100x100 image (20sp * 2 = 40px)
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    
    // Calculate text position (centered)
    val x = width / 2f
    val y = height / 2f - ((paint.descent() + paint.ascent()) / 2f)
    
    // Draw the letter
    canvas.drawText(difficultyLetter, x, y, paint)
    
    // Convert bitmap to base64
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val imageBytes = outputStream.toByteArray()
    val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
    
    return "data:image/png;base64,$base64String"
}

private val AppBackground = Color(0xFFF8F8F8)
private val CardSurface = Color(0xFFFFFFFF)
private val MutedText = Color(0xFF666666)

class QuizManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Validate role before showing screen - redirect to login if not admin role
        RoleValidationUtil.validateRoleAndRedirect(this, "admin") { isValid ->
            if (!isValid) {
                return@validateRoleAndRedirect // Will redirect to login
            }
            
            // Show screen only if role is valid
            setContent {
            val customSelectionColors = TextSelectionColors(
                handleColor = Color(0xFFFACC15),
                backgroundColor = Color(0x33FACC15)
            )

            CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
                QuizApp()
            }
            } // Close setContent
        } // Close validateRoleAndRedirect lambda
    } // Close onCreate
}

fun getTimeAgo(time: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - time
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "$days day${if (days > 1) "s" else ""} ago"
    }
}

fun randomPastTime(): Long {
    return System.currentTimeMillis() - (1..10).random() * 24 * 60 * 60 * 1000L
}

// Helper function to convert image to base64 data URI
suspend fun convertImageToBase64(context: Context, imageUri: Uri): Pair<String?, String?> {
    return try {
        val contentResolver: ContentResolver = context.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(imageUri)

        if (inputStream == null) {
            val errorMsg = "Failed to open input stream from URI: $imageUri"
            android.util.Log.e("QuizManagement", errorMsg)
            return Pair(null, errorMsg)
        }
        
        // Check image file size (max 5MB)
        val maxFileSize = 5 * 1024 * 1024 // 5MB in bytes
        val availableBytes = inputStream.available()
        if (availableBytes > maxFileSize) {
            inputStream.close()
            val errorMsg = "Image size exceeds 5MB limit. Please use a smaller image."
            android.util.Log.e("QuizManagement", errorMsg)
            return Pair(null, errorMsg)
        }

        inputStream.use { stream ->
            // For images, use bitmap conversion
            val bitmap = BitmapFactory.decodeStream(stream)

            if (bitmap == null) {
                val errorMsg = "Failed to decode image"
                android.util.Log.e("QuizManagement", errorMsg)
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
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val imageBytes = outputStream.toByteArray()

            // Convert to base64
            val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64String"

            Pair(dataUri, null)
        }
    } catch (e: Exception) {
        val errorMsg = "Error converting image: ${e.message ?: e.javaClass.simpleName}"
        android.util.Log.e("QuizManagement", errorMsg, e)
        Pair(null, errorMsg)
    }
}

@Composable
fun QuizApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val quizRepository = remember { QuizRepository() }
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val adminEmail = remember { sharedPref.getString("user_email", "") ?: "" }

    val allQuizzes = remember { mutableStateListOf<Quiz>() }

    // Load quizzes from Firebase on init
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val result = quizRepository.getQuizzes()
            result.onSuccess { quizzes ->
                withContext(Dispatchers.Main) {
                    allQuizzes.clear()
                    // Ensure we have a quiz for each difficulty level
                    Difficulty.values().forEach { diff ->
                        val existingQuiz = quizzes.find { it.difficulty == diff }
                        if (existingQuiz != null) {
                            allQuizzes.add(existingQuiz)
                        } else {
                            allQuizzes.add(Quiz(difficulty = diff))
                        }
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    // Initialize with empty quizzes for each difficulty
                    Difficulty.values().forEach { diff ->
                        allQuizzes.add(Quiz(difficulty = diff))
                    }
                    Toast.makeText(context, "Failed to load quizzes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopNav3(onTranslateClick = {
                context.startActivity(Intent(context, CommunitySpaceManagementActivity::class.java))
            })
        }, bottomBar = {
            BottomNav2(onLearnClick = {
                context.startActivity(
                    Intent(context, LearningManagementActivity::class.java)
                )
            }, onAnalyticsClick = {
                context.startActivity(
                    Intent(context, AnalyticsDashboardActivity::class.java)
                )
            }, onQuizClick = { /* already in quiz management */ })
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
                    val quizzesByDifficulty = Difficulty.values().map { diff ->
                        allQuizzes.firstOrNull { it.difficulty == diff } ?: Quiz(difficulty = diff)
                    }
                    QuizListScreen(quizzesByDifficulty = quizzesByDifficulty, onAddQuiz = { diff ->
                        val quiz =
                            allQuizzes.find { it.difficulty == diff } ?: Quiz(difficulty = diff)
                        if (!allQuizzes.contains(quiz)) allQuizzes.add(quiz)
                        navController.navigate("manage/${quiz.id}")
                    })
                }

                composable("manage/{quizId}") { backEntry ->
                    val quizId = backEntry.arguments?.getString("quizId") ?: return@composable
                    val quiz = allQuizzes.find { it.id == quizId } ?: return@composable

                            ManageQuizScreen(
                                quiz = quiz,
                                navController = navController, 
                                context = context,
                                quizRepository = quizRepository,
                                adminEmail = adminEmail,
                                onQuizUpdated = { updatedQuiz ->
                                    val index = allQuizzes.indexOfFirst { it.id == updatedQuiz.id }
                                    if (index != -1) {
                                        allQuizzes[index] = updatedQuiz
                                    } else {
                                        allQuizzes.add(updatedQuiz)
                                    }
                                },
                                onQuizDeleted = { deletedQuizId ->
                                    val quizToRemove = allQuizzes.find { it.id == deletedQuizId }
                                    if (quizToRemove != null) {
                                        allQuizzes.remove(quizToRemove)
                                    }
                                }
                            )
                }

                composable("addQuestion/{quizId}") { backEntry ->
                    val quizId = backEntry.arguments?.getString("quizId") ?: return@composable
                    val quiz = allQuizzes.find { it.id == quizId } ?: return@composable

                    if (quiz.questions.size >= 10) {
                        LaunchedEffect(Unit) {
                            Toast.makeText(
                                context, "Maximum 10 questions allowed", Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@composable
                    }

                    val newQuestion = Question()
                    QuestionEditorScreen(
                        title = "Add Question",
                        question = newQuestion,
                        quiz = quiz,
                        quizRepository = quizRepository,
                        adminEmail = adminEmail,
                        onSaveConfirmed = { question ->
                            // Update UI immediately (real-time)
                            quiz.questions.add(question)
                            if (quiz.id.length > 20) {
                                quiz.lastUpdated = System.currentTimeMillis()
                            }
                            // Update local state immediately
                            val index = allQuizzes.indexOfFirst { it.difficulty == quiz.difficulty }
                            if (index != -1) {
                                allQuizzes[index] = quiz
                            } else {
                                allQuizzes.add(quiz)
                            }
                            
                            // Validate quiz has at least 1 question before saving
                            if (quiz.questions.isNotEmpty()) {
                                // Save to Firebase in background (non-blocking)
                                CoroutineScope(Dispatchers.IO).launch {
                                    val result = quizRepository.saveQuiz(quiz, adminEmail)
                                    withContext(Dispatchers.Main) {
                                        if (result.isSuccess) {
                                            // Update quiz ID if it was a new quiz
                                            val savedQuizId = result.getOrNull()
                                            if (savedQuizId != null && quiz.id != savedQuizId) {
                                                quiz.id = savedQuizId
                                                val updateIndex = allQuizzes.indexOfFirst { it.difficulty == quiz.difficulty }
                                                if (updateIndex != -1) {
                                                    allQuizzes[updateIndex] = quiz
                                                }
                                            }
                                            Toast.makeText(context, "Question added and saved", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        } else {
                                            Toast.makeText(context, "Failed to save: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Quiz must have at least 1 question", Toast.LENGTH_SHORT).show()
                            }
                        },
                        isAdd = true
                    )
                }

                composable("editQuestion/{quizId}/{questionId}") { backEntry ->
                    val quizId = backEntry.arguments?.getString("quizId") ?: return@composable
                    val questionId =
                        backEntry.arguments?.getString("questionId") ?: return@composable
                    val quiz = allQuizzes.find { it.id == quizId } ?: return@composable
                    val question = quiz.questions.find { it.id == questionId } ?: return@composable

                    QuestionEditorScreen(
                        title = "Edit Question", 
                        question = question,
                        quiz = quiz,
                        quizRepository = quizRepository,
                        adminEmail = adminEmail,
                        onSaveConfirmed = { updated ->
                            // Update UI immediately (real-time)
                            val index = quiz.questions.indexOfFirst { it.id == updated.id }
                            if (index != -1) quiz.questions[index] = updated
                            if (quiz.id.length > 20) {
                                quiz.lastUpdated = System.currentTimeMillis()
                            }
                            // Update local state immediately
                            val quizIndex = allQuizzes.indexOfFirst { it.difficulty == quiz.difficulty }
                            if (quizIndex != -1) {
                                allQuizzes[quizIndex] = quiz
                            }
                            
                            // Validate quiz has at least 1 question before saving
                            if (quiz.questions.isNotEmpty()) {
                                // Save to Firebase in background (non-blocking)
                                CoroutineScope(Dispatchers.IO).launch {
                                    val result = quizRepository.saveQuiz(quiz, adminEmail)
                                    withContext(Dispatchers.Main) {
                                        if (result.isSuccess) {
                                            // Update quiz ID if changed
                                            val savedQuizId = result.getOrNull()
                                            if (savedQuizId != null && quiz.id != savedQuizId) {
                                                quiz.id = savedQuizId
                                                val updateIndex = allQuizzes.indexOfFirst { it.difficulty == quiz.difficulty }
                                                if (updateIndex != -1) {
                                                    allQuizzes[updateIndex] = quiz
                                                }
                                            }
                                            Toast.makeText(context, "Question updated and saved", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        } else {
                                            Toast.makeText(context, "Failed to save: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Quiz must have at least 1 question", Toast.LENGTH_SHORT).show()
                            }
                        }, 
                        isAdd = false
                    )
                }
            }
        }
    }
}

@Composable
fun QuizListScreen(quizzesByDifficulty: List<Quiz>, onAddQuiz: (Difficulty) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp)
    ) {
        Text(
            "Quizzes",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = InterFontFamily
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Add quiz for a difficulty level",
            fontSize = 14.sp,
            color = MutedText,
            fontFamily = InterFontFamily
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(quizzesByDifficulty) { quiz ->
                QuizCard(quiz = quiz, onAdd = { onAddQuiz(quiz.difficulty) })
            }
        }
    }
}

@Composable
fun QuizCard(quiz: Quiz, onAdd: () -> Unit) {
    val difficultyLetter = when (quiz.difficulty) {
        Difficulty.EASY -> "E"
        Difficulty.MEDIUM -> "M"
        Difficulty.DIFFICULT -> "D"
        Difficulty.PRO -> "P"
    }

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
                Text(
                    difficultyLetter,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                DifficultyBadge(quiz.difficulty)
                Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${quiz.questions.size} question(s)",
                            color = MutedText,
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily
                        )
                        if (quiz.lastUpdated > 0) {
                            Text(
                                "Updated ${getTimeAgo(quiz.lastUpdated)}\n${formatDate(quiz.lastUpdated)}",
                                color = MutedText,
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily
                            )
                        }
            }

            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, tint = Color.Black, contentDescription = "Add Quiz")
            }
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: Difficulty) {
    val bg = when (difficulty) {
        Difficulty.EASY -> Color(0xFFEFFDF0)
        Difficulty.MEDIUM -> Color(0xFFFFFBEA)
        Difficulty.DIFFICULT -> Color(0xFFFFF0EE)
        Difficulty.PRO -> Color(0xFFF6F2FF)
    }
    val animated by animateColorAsState(
        targetValue = bg, animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
    Surface(shape = RoundedCornerShape(8.dp), color = animated) {
        Text(
            difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily
        )
    }
}

@Composable
fun ManageQuizScreen(
    quiz: Quiz, 
    navController: NavController, 
    context: android.content.Context,
    quizRepository: QuizRepository,
    adminEmail: String,
    onQuizUpdated: (Quiz) -> Unit,
    onQuizDeleted: (String) -> Unit
) {
    val deleteDialog = remember { mutableStateOf<Pair<Boolean, String?>>(false to null) }
    // Track questions count to force recomposition when questions are deleted
    val questionsCount = remember(quiz.questions.size) { mutableStateOf(quiz.questions.size) }
    val isLoading = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // AI generation state
    val showAIGenerationDialog = remember { mutableStateOf(false) }
    val aiGenerationCount = remember { mutableStateOf("5") }
    val isGeneratingAI = remember { mutableStateOf(false) }
    val aiQuizService = remember { AIQuizService() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Manage Quiz",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // AI Generate Questions Button
            val remainingSlots = 10 - quiz.questions.size
            val canGenerate = remainingSlots > 0
            IconButton(
                onClick = {
                    if (canGenerate) {
                        showAIGenerationDialog.value = true
                    } else {
                        Toast.makeText(context, "Maximum 10 questions reached", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = canGenerate
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI Generate Questions",
                    tint = if (canGenerate) Color(0xFFFACC15) else Color.Gray
                )
            }
            
            // Manual Add Question Button
            IconButton(
                onClick = {
                    if (quiz.questions.size < 10) navController.navigate("addQuestion/${quiz.id}")
                    else Toast.makeText(context, "Maximum 10 questions reached", Toast.LENGTH_SHORT)
                        .show()
                }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Question",
                    tint = if (quiz.questions.size < 10) Color.Black else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Questions (${questionsCount.value})", fontFamily = InterFontFamily, color = MutedText
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Use LaunchedEffect to watch for changes in questions list
        LaunchedEffect(quiz.questions.size) {
            questionsCount.value = quiz.questions.size
        }
        
        Box(modifier = Modifier.weight(1f)) {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (quiz.questions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No quiz available yet.",
                                    textAlign = TextAlign.Center,
                                    color = MutedText,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    } else {
                        items(
                            items = quiz.questions,
                            key = { it.id }
                        ) { question ->
                            QuestionCard(
                                question = question,
                                onEdit = { navController.navigate("editQuestion/${quiz.id}/${question.id}") },
                                onDelete = { deleteDialog.value = true to question.id })
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
                        "AI Generate Questions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Generate quiz questions automatically using AI. Questions will be created based on the ${quiz.difficulty.name.lowercase()} difficulty level.\n\nQuestions are about identifying sign language from images - users will see sign language pictures and identify what the sign means or what sign language it is (e.g., \"What does this sign mean?\" - showing a 'thank you' sign).\n\nQuestions cover ASL and FSL signs like \"thank you\", \"hello\", \"goodbye\", \"yes\", \"no\", \"please\", \"sorry\", \"love\", \"family\", etc.\n\nNote: Generated questions will have placeholder images. You can edit each question later to add actual sign language images and validate them.",
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val remainingSlots = 10 - quiz.questions.size
                    OutlinedTextField(
                        value = aiGenerationCount.value,
                        onValueChange = { newValue ->
                            val num = newValue.toIntOrNull()
                            if (num != null && num > 0 && num <= remainingSlots) {
                                aiGenerationCount.value = newValue
                            } else if (newValue.isEmpty()) {
                                aiGenerationCount.value = ""
                            }
                        },
                        label = {
                            Text("Number of Questions (1-$remainingSlots)", fontFamily = InterFontFamily, color = Color(0xFF666666))
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
                                "Maximum $remainingSlots questions can be generated",
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
                                color = Color(0xFFFACC15)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Generating questions...",
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
                                    val count = aiGenerationCount.value.toIntOrNull() ?: return@Button
                                    if (count <= 0 || count > remainingSlots) {
                                        Toast.makeText(context, "Please enter a valid number between 1 and $remainingSlots", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    isGeneratingAI.value = true
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val result = aiQuizService.generateQuizQuestions(
                                                difficulty = quiz.difficulty.name,
                                                count = count
                                            )
                                            
                                            withContext(Dispatchers.Main) {
                                                isGeneratingAI.value = false
                                                showAIGenerationDialog.value = false
                                                
                                                result.onSuccess { aiResponse ->
                                                    if (aiResponse.success && aiResponse.questions != null) {
                                                        var addedCount = 0
                                                        aiResponse.questions.forEach { generatedQuestion ->
                                                            if (quiz.questions.size < 10) {
                                                                // Create a colored placeholder image based on difficulty level
                                                                val placeholderImageBase64 = createDifficultyPlaceholderImage(quiz.difficulty)
                                                                val placeholderImageUri = android.net.Uri.parse(placeholderImageBase64)
                                                                
                                                                val newQuestion = Question(
                                                                    text = generatedQuestion.question,
                                                                    correctAnswer = generatedQuestion.correctAnswer,
                                                                    wrongOptions = generatedQuestion.wrongOptions.toMutableList(),
                                                                    imageUri = placeholderImageUri
                                                                )
                                                                
                                                                quiz.questions.add(newQuestion)
                                                                addedCount++
                                                            }
                                                        }
                                                        
                                                        if (addedCount > 0) {
                                                            questionsCount.value = quiz.questions.size
                                                            if (quiz.id.length > 20) {
                                                                quiz.lastUpdated = System.currentTimeMillis()
                                                            }
                                                            
                                                            // Show loading indicator while saving
                                                            isLoading.value = true
                                                            
                                                            // Save to Firebase
                                                            scope.launch(Dispatchers.IO) {
                                                                val saveResult = quizRepository.saveQuiz(quiz, adminEmail)
                                                                withContext(Dispatchers.Main) {
                                                                    isLoading.value = false
                                                                    
                                                                    saveResult.onSuccess {
                                                                        // Reload quiz from Firebase to get updated data
                                                                        scope.launch(Dispatchers.IO) {
                                                                            val reloadResult = quizRepository.getQuizByDifficulty(quiz.difficulty)
                                                                            withContext(Dispatchers.Main) {
                                                                                if (reloadResult.isSuccess) {
                                                                                    val reloadedQuiz = reloadResult.getOrNull()
                                                                                    if (reloadedQuiz != null) {
                                                                                        // Update quiz with fresh data from Firebase
                                                                                        quiz.questions.clear()
                                                                                        quiz.questions.addAll(reloadedQuiz.questions)
                                                                                        questionsCount.value = quiz.questions.size
                                                                                        quiz.lastUpdated = reloadedQuiz.lastUpdated
                                                                                    }
                                                                                }
                                                                                onQuizUpdated(quiz)
                                                                                Toast.makeText(
                                                                                    context,
                                                                                    "Successfully generated and added $addedCount question(s)!",
                                                                                    Toast.LENGTH_SHORT
                                                                                ).show()
                                                                            }
                                                                        }
                                                                    }.onFailure { e ->
                                                                        Toast.makeText(
                                                                            context,
                                                                            "Questions added but failed to save: ${e.message}",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Cannot add more questions. Maximum 10 questions reached.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            aiResponse.message ?: "Failed to generate questions",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }.onFailure { e ->
                                                    Toast.makeText(
                                                        context,
                                                        "Error generating questions: ${e.message}",
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
                                    containerColor = Color(0xFFFACC15),
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
            title = "Delete Question",
            message = "Are you sure you want to delete this question?",
            confirmText = "Delete",
            confirmColor = Color.Red,
            onDismiss = { deleteDialog.value = false to null },
            onConfirm = {
                val questionIdToDelete = deleteDialog.value.second
                deleteDialog.value = false to null
                
                // Show loading indicator
                isLoading.value = true
                
                // Remove from UI immediately (real-time update)
                val removed = quiz.questions.removeAll { it.id == questionIdToDelete }
                
                if (removed) {
                    // Update questions count to force recomposition
                    questionsCount.value = quiz.questions.size
                    
                    // Update quiz in Firebase immediately (real-time)
                    if (quiz.id.length > 20) { // Only update if quiz exists
                        quiz.lastUpdated = System.currentTimeMillis()
                    }
                    onQuizUpdated(quiz) // Update UI immediately - no delay, stays on manage quiz screen
                }
                
                // Save to Firebase and reload quiz data
                scope.launch(Dispatchers.IO) {
                    if (quiz.questions.isEmpty() && quiz.id.length > 20) {
                        // Validate quiz exists before deleting
                        val quizExists = try {
                            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val existingQuiz = firestore.collection("quizzes")
                                .document(quiz.id)
                                .get()
                                .await()
                            existingQuiz.exists()
                        } catch (e: Exception) {
                            false
                        }
                        
                        if (!quizExists) {
                            withContext(Dispatchers.Main) {
                                isLoading.value = false
                                Toast.makeText(context, "Quiz not found. It may have been already deleted.", Toast.LENGTH_SHORT).show()
                                onQuizDeleted(quiz.id) // Remove from local list anyway
                                // Navigate to quiz list screen
                                navController.navigate("list") {
                                    popUpTo("list") { inclusive = false }
                                }
                            }
                            return@launch
                        }
                        
                        // Delete entire quiz from Firebase if no questions left
                        val result = quizRepository.deleteQuiz(quiz.id)
                        withContext(Dispatchers.Main) {
                            isLoading.value = false
                            if (result.isSuccess) {
                                // Remove from local list via callback
                                onQuizDeleted(quiz.id)
                                Toast.makeText(context, "Quiz deleted (no questions remaining)", Toast.LENGTH_SHORT).show()
                                // Navigate to quiz list screen
                                navController.navigate("list") {
                                    popUpTo("list") { inclusive = false }
                                }
                            } else {
                                Toast.makeText(context, "Failed to delete: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Update quiz in Firebase - DO NOT navigate, stay on manage quiz screen
                        val result = quizRepository.saveQuiz(quiz, adminEmail)
                        
                        // After saving, reload the quiz from Firebase to get fresh data
                        if (result.isSuccess) {
                            val reloadResult = quizRepository.getQuizByDifficulty(quiz.difficulty)
                            withContext(Dispatchers.Main) {
                                isLoading.value = false
                                if (reloadResult.isSuccess) {
                                    val reloadedQuiz = reloadResult.getOrNull()
                                    if (reloadedQuiz != null) {
                                        // Update quiz with fresh data from Firebase
                                        quiz.questions.clear()
                                        quiz.questions.addAll(reloadedQuiz.questions)
                                        quiz.id = reloadedQuiz.id
                                        quiz.lastUpdated = reloadedQuiz.lastUpdated
                                        questionsCount.value = quiz.questions.size
                                        onQuizUpdated(quiz) // Update UI with fresh data
                                        Toast.makeText(context, "Question deleted and saved", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Question deleted and saved", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Question deleted and saved", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                isLoading.value = false
                                Toast.makeText(context, "Failed to save: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
    }
}

@SuppressLint("RememberReturnType")
@Composable
fun QuestionEditorScreen(
    title: String, 
    question: Question,
    quiz: Quiz,
    quizRepository: QuizRepository,
    adminEmail: String,
    onSaveConfirmed: (Question) -> Unit, 
    isAdd: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var questionText by rememberSaveable { mutableStateOf(question.text) }
    var correctAnswer by rememberSaveable { mutableStateOf(question.correctAnswer) }
    var wrongOptionsText by rememberSaveable { mutableStateOf(question.wrongOptions.joinToString(", ")) }
    // Check if imageUri is already a base64 data URI
    val initialImageUri = question.imageUri
    val initialImageUriString = initialImageUri?.toString() ?: ""
    val isBase64Uri = initialImageUriString.startsWith("data:image")
    var localImageUri by rememberSaveable { 
        mutableStateOf(if (isBase64Uri) null else initialImageUri) 
    }
    var imageBase64Uri by rememberSaveable { 
        mutableStateOf<String?>(if (isBase64Uri) initialImageUriString else null) 
    }
    var isProcessingImage by remember { mutableStateOf(false) }
    val saveDialogVisible = remember { mutableStateOf(false) }

            val imagePicker =
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri != null) {
                        // Check if it's an image (not GIF)
                        val mimeType = context.contentResolver.getType(uri) ?: ""
                        if (!mimeType.contains("gif", ignoreCase = true) && !uri.toString().lowercase().endsWith(".gif")) {
                            localImageUri = uri
                            imageBase64Uri = null
                            // Convert to base64 in background
                            isProcessingImage = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val (base64Uri, error) = convertImageToBase64(context, uri)
                                withContext(Dispatchers.Main) {
                                    isProcessingImage = false
                                    if (base64Uri != null) {
                                        imageBase64Uri = base64Uri
                                    } else {
                                        Toast.makeText(context, "Failed to process image: ${error ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "GIFs are not supported. Please select an image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

    val isSaveEnabled by remember(questionText, correctAnswer, wrongOptionsText, localImageUri, imageBase64Uri) {
        derivedStateOf {
            val wrongOptionsList = wrongOptionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val hasValidWrongOptions = wrongOptionsList.size >= 1 && wrongOptionsList.size <= 3
            val hasImage = imageBase64Uri != null || localImageUri != null
            
            // Validate text lengths
            val questionTextTrimmed = questionText.trim()
            val correctAnswerTrimmed = correctAnswer.trim()
            val maxQuestionLength = 500
            val maxAnswerLength = 10 // Choices must be one word, max 10 characters to fit in design
            // Validate that answers are one word (no spaces) and within length limit
            val isValidAnswerLength = correctAnswerTrimmed.length <= maxAnswerLength && 
                correctAnswerTrimmed.isNotEmpty() && 
                !correctAnswerTrimmed.contains(" ") // Must be one word only
            val isValidWrongOptionsLength = wrongOptionsList.all { 
                it.length <= maxAnswerLength && !it.contains(" ") // Must be one word only
            }
            val isValidQuestionLength = questionTextTrimmed.length <= maxQuestionLength && questionTextTrimmed.isNotEmpty()
            
            // Validate image size (base64 max ~5MB = ~6.67MB base64 string)
            val maxImageSize = 6_700_000 // ~5MB in base64
            val currentImageBase64Uri = imageBase64Uri // Local variable for smart cast
            val isValidImageSize = if (currentImageBase64Uri != null) {
                currentImageBase64Uri.length <= maxImageSize
            } else {
                true // Local URI size will be checked when converting
            }
            
            val allFieldsFilled =
                isValidQuestionLength && isValidAnswerLength && hasValidWrongOptions && hasImage && isValidWrongOptionsLength && isValidImageSize
            
            if (isAdd) {
                allFieldsFilled
            } else {
                val originalWrongOptionsText = question.wrongOptions.joinToString(", ")
                val currentLocalUri = localImageUri
                val currentImageBase64UriForCheck = imageBase64Uri // Local variable for smart cast
                val imageChanged = if (currentImageBase64UriForCheck != null) {
                    // New base64 image was set
                    currentImageBase64UriForCheck != (question.imageUri?.toString()?.takeIf { it.startsWith("data:image") })
                } else {
                    // Check if local URI changed
                    currentLocalUri != question.imageUri
                }
                
                allFieldsFilled && (
                    questionText != question.text || 
                    correctAnswer != question.correctAnswer || 
                    wrongOptionsText != originalWrongOptionsText ||
                    imageChanged
                )
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
                    contentDescription = "Save Question",
                    tint = if (isSaveEnabled) Color.Black else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = questionText, 
            onValueChange = { 
                if (it.length <= 500) {
                    questionText = it
                } else {
                    Toast.makeText(context, "Question text cannot exceed 500 characters", Toast.LENGTH_SHORT).show()
                }
            }, 
            label = {
                Text("Question", fontFamily = InterFontFamily, color = Color(0xFF666666))
            }, 
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
                    "${questionText.trim().length}/500 characters",
                    fontFamily = InterFontFamily,
                    color = if (questionText.trim().length > 500) Color.Red else Color(0xFF666666)
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F4F7)), contentAlignment = Alignment.Center
        ) {
            if (isProcessingImage) {
                Text("Processing image...", color = MutedText, fontFamily = InterFontFamily)
            } else {
                val bitmap = remember(imageBase64Uri, localImageUri) {
                    val base64Uri = imageBase64Uri
                    val localUri = localImageUri
                    when {
                        base64Uri != null && base64Uri.startsWith("data:image") -> {
                            try {
                                val base64String = base64Uri.substringAfter(",")
                                if (base64String.isNotEmpty()) {
                                    val imageBytes = Base64.decode(base64String, Base64.NO_WRAP)
                                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }
                        localUri != null -> {
                            try {
                                val inputStream = context.contentResolver.openInputStream(localUri)
                                inputStream?.use { BitmapFactory.decodeStream(it) }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        else -> null
                    }
                }
                
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Upload sign image", color = MutedText, fontFamily = InterFontFamily)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { imagePicker.launch("image/*") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = correctAnswer, 
            onValueChange = { newValue ->
                // Only allow one word (no spaces) and max 10 characters
                val trimmed = newValue.trim()
                if (trimmed.contains(" ")) {
                    // Take only the first word if space is detected
                    val firstWord = trimmed.split(" ")[0]
                    if (firstWord.length <= 10) {
                        correctAnswer = firstWord
                    } else {
                        Toast.makeText(context, "Answer must be one word, max 10 characters", Toast.LENGTH_SHORT).show()
                    }
                } else if (newValue.length <= 10) {
                    correctAnswer = newValue
                } else {
                    Toast.makeText(context, "Answer must be one word, max 10 characters", Toast.LENGTH_SHORT).show()
                }
            }, 
            label = {
                Text("Correct Answer (one word, max 10 chars)", fontFamily = InterFontFamily, color = Color(0xFF666666))
            }, 
            modifier = Modifier.fillMaxWidth(), 
            colors = OutlinedTextFieldDefaults.colors(
                cursorColor = Color.Black,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color(0xFF666666),
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color(0xFF666666)
            ),
            supportingText = {
                val hasSpace = correctAnswer.contains(" ")
                Text(
                    "${correctAnswer.trim().length}/10 characters${if (hasSpace) " (spaces removed)" else ""}",
                    fontFamily = InterFontFamily,
                    color = when {
                        correctAnswer.trim().length > 10 -> Color.Red
                        hasSpace -> Color(0xFFFF9800) // Orange warning for spaces
                        else -> Color(0xFF666666)
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = wrongOptionsText,
            onValueChange = { newValue ->
                // Process each option: take first word only, max 10 chars
                val processedOptions = newValue.split(",").map { option ->
                    val trimmed = option.trim()
                    if (trimmed.contains(" ")) {
                        // Take only the first word if space is detected
                        trimmed.split(" ")[0].take(10)
                    } else {
                        trimmed.take(10)
                    }
                }
                
                // Reconstruct the text with processed options
                val processedText = processedOptions.joinToString(", ")
                
                // Count current options (before change)
                val currentOptions = wrongOptionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                // Count new options (after change)
                val newOptions = processedOptions.filter { it.isNotEmpty() }
                
                // Check if any option exceeds 10 characters or has spaces (shouldn't happen after processing, but check original)
                val originalOptions = newValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val hasInvalidOptions = originalOptions.any { it.length > 10 || it.contains(" ") }
                if (hasInvalidOptions && newValue != wrongOptionsText) {
                    // Show warning but allow the processed version
                    Toast.makeText(context, "Each option must be one word, max 10 characters", Toast.LENGTH_SHORT).show()
                }
                
                // Check if correct answer is being added to wrong options
                val correctAnswerTrimmed = correctAnswer.trim()
                if (correctAnswerTrimmed.isNotEmpty()) {
                    val containsCorrectAnswer = newOptions.any { it.equals(correctAnswerTrimmed, ignoreCase = true) }
                    if (containsCorrectAnswer) {
                        Toast.makeText(context, "Correct answer cannot be used as a wrong option", Toast.LENGTH_SHORT).show()
                        return@OutlinedTextField
                    }
                }
                
                // Check for duplicate wrong options (case-insensitive)
                val duplicateOptions = newOptions.groupingBy { it.lowercase() }.eachCount().filter { it.value > 1 }
                if (duplicateOptions.isNotEmpty()) {
                    Toast.makeText(context, "Duplicate wrong options are not allowed", Toast.LENGTH_SHORT).show()
                    return@OutlinedTextField
                }
                
                // If already have 3 options, prevent adding more commas
                if (currentOptions.size >= 3) {
                    // Count commas in current and new value
                    val currentCommaCount = wrongOptionsText.count { it == ',' }
                    val newCommaCount = processedText.count { it == ',' }
                    
                    // If trying to add a comma when already at 3 options, prevent it
                    if (newCommaCount > currentCommaCount) {
                        Toast.makeText(context, "Maximum 3 wrong options allowed", Toast.LENGTH_SHORT).show()
                        return@OutlinedTextField
                    }
                }
                
                // Limit to 3 wrong options
                if (newOptions.size <= 3) {
                    wrongOptionsText = processedText
                } else {
                    Toast.makeText(context, "Maximum 3 wrong options allowed", Toast.LENGTH_SHORT).show()
                }
            },
            label = {
                Text(
                    "Wrong Options (comma separated, max 3, one word each, max 10 chars)",
                    fontFamily = InterFontFamily,
                    color = Color(0xFF666666)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                cursorColor = Color.Black,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color(0xFF666666),
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color(0xFF666666)
            ),
            supportingText = {
                val options = wrongOptionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val optionCount = options.size
                val hasInvalidOptions = options.any { it.length > 10 || it.contains(" ") }
                val invalidCount = options.count { it.length > 10 || it.contains(" ") }
                Text(
                    "$optionCount/3 options${if (hasInvalidOptions) " • $invalidCount invalid (max 10 chars, one word)" else ""}",
                    fontFamily = InterFontFamily,
                    color = when {
                        optionCount > 3 -> Color.Red
                        hasInvalidOptions -> Color(0xFFFF9800) // Orange warning
                        else -> Color(0xFF666666)
                    }
                )
            }
        )
    }

    if (saveDialogVisible.value) {
        ConfirmStyledDialog(
            title = "Save Question",
            message = "Do you want to save this question?",
            confirmText = "Save",
            confirmColor = Color(0xFFFACC15),
            confirmTextColor = Color.Black,
            onDismiss = { saveDialogVisible.value = false },
            onConfirm = {
                saveDialogVisible.value = false
                // Use base64 URI if available, otherwise use local URI
                val currentImageBase64UriForSave = imageBase64Uri // Local variable for smart cast
                val finalImageUri = if (currentImageBase64UriForSave != null) {
                    // Store base64 URI as string in the question
                    android.net.Uri.parse(currentImageBase64UriForSave)
                } else {
                    localImageUri
                }
                
                val wrongOptionsList = wrongOptionsText.split(",").map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3) // Ensure max 3 options
                    .toMutableList()
                
                onSaveConfirmed(
                    question.copy(
                        text = questionText,
                        imageUri = finalImageUri,
                        correctAnswer = correctAnswer.trim(),
                        wrongOptions = wrongOptionsList
                    )
                )
            })
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
                    ) { Text("Cancel", color = Color(0xFF666666)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm, colors = ButtonDefaults.buttonColors(
                            containerColor = confirmColor, contentColor = confirmTextColor
                        )
                    ) { Text(confirmText) }
                }
            }
        }
    }
}

@Composable
fun QuestionCard(question: Question, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF2F4F7))
            ) {
                if (question.imageUri != null && question.imageUri.toString().isNotEmpty()) {
                    val context = LocalContext.current
                    val imageData = if (question.imageUri.toString().startsWith("data:image")) {
                        question.imageUri.toString()
                    } else {
                        question.imageUri
                    }
                    
                    // Try to decode as bitmap first
                    val bitmap = remember(imageData) {
                        if (imageData.toString().startsWith("data:image")) {
                            try {
                                val base64String = imageData.toString().substringAfter(",")
                                if (base64String.isNotEmpty()) {
                                    val imageBytes = Base64.decode(base64String, Base64.NO_WRAP)
                                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            try {
                                val inputStream = context.contentResolver.openInputStream(imageData as Uri)
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
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback to AsyncImage if bitmap decoding fails
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(imageData)
                                .crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    question.text.ifBlank { "Question Text" },
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily
                )
                Text(
                    "Correct: ${question.correctAnswer.ifBlank { "—" }}",
                    color = MutedText,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily
                )
                Text(
                    "Wrong: ${question.wrongOptions.joinToString(", ")}",
                    color = MutedText,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red
                )
            }
        }
    }
}
