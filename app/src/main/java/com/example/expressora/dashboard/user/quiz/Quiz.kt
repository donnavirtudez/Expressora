package com.example.expressora.dashboard.user.quiz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.expressora.R
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.expressora.backend.UserQuizRepository
import com.example.expressora.components.top_nav3.TopNav3
import com.example.expressora.components.user_bottom_nav.BottomNav
import com.example.expressora.dashboard.admin.quizmanagement.Difficulty
import com.example.expressora.dashboard.admin.quizmanagement.Question
import com.example.expressora.dashboard.admin.quizmanagement.Quiz
import com.example.expressora.dashboard.user.community_space.CommunitySpaceActivity
import com.example.expressora.dashboard.user.learn.LearnActivity
import com.example.expressora.dashboard.user.translation.TranslationActivity
import com.example.expressora.models.QuizAnswer
import com.example.expressora.models.QuizAttempt
import com.example.expressora.models.QuizProgress
import com.example.expressora.ui.theme.InterFontFamily
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class QuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuizApp() }
    }
}

@Composable
fun QuizApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val userQuizRepository = remember { UserQuizRepository() }
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userEmail = remember { sharedPref.getString("user_email", "") ?: "" }
    val userRole = remember { sharedPref.getString("user_role", "user") ?: "user" }
    
    var selectedDifficulty by remember { mutableStateOf("") }
    var currentQuiz by remember { mutableStateOf<Quiz?>(null) }
    var userBestScores by remember { mutableStateOf<Map<String, QuizAttempt?>>(emptyMap()) }
    var isLoadingQuizzes by remember { mutableStateOf(false) }
    
    // Load user's best scores for each difficulty
    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val userId = getUserIdFromEmail(userEmail, userRole)
                if (userId != null) {
                    val scores = mutableMapOf<String, QuizAttempt?>()
                    listOf("Easy", "Medium", "Difficult", "Pro").forEach { diff ->
                        val result = userQuizRepository.getUserBestScore(userId, diff)
                        result.onSuccess { attempt ->
                            scores[diff] = attempt
                        }
                    }
                    withContext(Dispatchers.Main) {
                        userBestScores = scores
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopNav3(onTranslateClick = {
                context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
            })
        }, bottomBar = {
            BottomNav(onLearnClick = {
                context.startActivity(
                    Intent(
                        context, LearnActivity::class.java
                    )
                )
            }, onCameraClick = {
                context.startActivity(
                    Intent(
                        context, TranslationActivity::class.java
                    )
                )
            }, onQuizClick = { /* already in quiz */ })
        }, containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = "difficulty") {

                composable("difficulty") {
                    QuizDifficultyScreen(
                        selectedDifficulty = selectedDifficulty,
                        userBestScores = userBestScores,
                        userQuizRepository = userQuizRepository,
                        onDifficultySelected = { difficulty ->
                            selectedDifficulty = difficulty
                            // Load quiz and check for progress
                            CoroutineScope(Dispatchers.IO).launch {
                                val userId = getUserIdFromEmail(userEmail, userRole)
                                
                                // Check if level is unlocked
                                if (userId != null) {
                                    val isUnlocked = when (difficulty) {
                                        "Easy" -> true
                                        "Medium" -> {
                                            val easyResult = userQuizRepository.getUserBestScore(userId, "Easy")
                                            (easyResult.getOrNull()?.percentage ?: 0.0) >= 100.0
                                        }
                                        "Difficult" -> {
                                            val mediumResult = userQuizRepository.getUserBestScore(userId, "Medium")
                                            (mediumResult.getOrNull()?.percentage ?: 0.0) >= 100.0
                                        }
                                        "Pro" -> {
                                            val difficultResult = userQuizRepository.getUserBestScore(userId, "Difficult")
                                            (difficultResult.getOrNull()?.percentage ?: 0.0) >= 100.0
                                        }
                                        else -> false
                                    }
                                    
                                    if (!isUnlocked) {
                                        withContext(Dispatchers.Main) {
                                            val previousLevel = when (difficulty) {
                                                "Medium" -> "Easy"
                                                "Difficult" -> "Medium"
                                                "Pro" -> "Difficult"
                                                else -> ""
                                            }
                                            Toast.makeText(context, "$difficulty level is locked! Get a perfect score in $previousLevel level to unlock.", Toast.LENGTH_LONG).show()
                                        }
                                        return@launch
                                    }
                                    
                                    // Check if level is already perfect (prevent retake)
                                    val bestResult = userQuizRepository.getUserBestScore(userId, difficulty)
                                    bestResult.onSuccess { bestAttempt ->
                                        val isPerfect = bestAttempt != null && (bestAttempt.percentage ?: 0.0) >= 100.0
                                        
                                        if (isPerfect) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "You already have a perfect score in $difficulty level! Next level is unlocked.", Toast.LENGTH_LONG).show()
                                            }
                                            return@launch
                                        }
                                    }
                                }
                                
                                val quizResult = userQuizRepository.getQuizByDifficulty(difficulty)
                                
                                withContext(Dispatchers.Main) {
                                    quizResult.onSuccess { quiz ->
                                        if (quiz != null && quiz.questions.isNotEmpty()) {
                                            // Check for existing progress
                                            if (userId != null) {
                                                val progressResult = userQuizRepository.getQuizProgress(userId, difficulty)
                                                progressResult.onSuccess { progress ->
                                                    if (progress != null) {
                                                        // Check if quiz was updated (different question count or IDs)
                                                        val currentQuestionIds = quiz.questions.map { it.id }.toSet()
                                                        val savedQuestionIds = progress.questionOrder.toSet()
                                                        
                                                        // If quiz structure changed (questions added/removed/changed), reset progress
                                                        val quizWasUpdated = currentQuestionIds != savedQuestionIds || 
                                                                             quiz.questions.size != progress.totalQuestions
                                                        
                                                        if (quizWasUpdated) {
                                                            // Quiz was updated - delete old progress and start fresh
                                                            val oldQuestionCount = progress.totalQuestions
                                                            val newQuestionCount = quiz.questions.size
                                                            
                                                            withContext(Dispatchers.IO) {
                                                                userQuizRepository.deleteQuizProgress(userId, difficulty)
                                                            }
                                                            
                                                            // Show notification to user with specific details
                                                            val message = if (oldQuestionCount != newQuestionCount) {
                                                                "Quiz updated: $oldQuestionCount → $newQuestionCount questions. Starting fresh with the latest version."
                                                            } else {
                                                                "Quiz was updated. Starting fresh with the latest version."
                                                            }
                                                            
                                                            // Show toast notification
                                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                            
                                                            // Start fresh with shuffled questions
                                                            val shuffledQuestions = quiz.questions.shuffled().toMutableList()
                                                            currentQuiz = quiz.copy(questions = shuffledQuestions)
                                                            
                                                            // Small delay to ensure toast is visible before navigation
                                                            CoroutineScope(Dispatchers.Main).launch {
                                                                delay(500) // 500ms delay
                                                                navController.navigate("question/0/0")
                                                            }
                                                        } else {
                                                            // Quiz unchanged - continue from exact same progress (same question order, same index, same score)
                                                            // Use saved question order to continue from where user left off
                                                            val orderedQuestions = progress.questionOrder.mapNotNull { qId ->
                                                                quiz.questions.find { it.id == qId }
                                                            }.toMutableList()
                                                            
                                                            // Handle invalid question IDs - if progress has invalid IDs, reset progress
                                                            if (orderedQuestions.isEmpty() || orderedQuestions.size < progress.questionOrder.size) {
                                                                // Some question IDs are invalid - reset progress and start fresh
                                                                withContext(Dispatchers.IO) {
                                                                    userQuizRepository.deleteQuizProgress(userId, difficulty)
                                                                }
                                                                Toast.makeText(context, "Some questions were removed. Starting fresh.", Toast.LENGTH_LONG).show()
                                                                val shuffledQuestions = quiz.questions.shuffled().toMutableList()
                                                                currentQuiz = quiz.copy(questions = shuffledQuestions)
                                                                CoroutineScope(Dispatchers.Main).launch {
                                                                    delay(500)
                                                                    navController.navigate("question/0/0")
                                                                }
                                                            } else {
                                                                // Add any new questions that weren't in progress (shouldn't happen if quiz unchanged, but safety check)
                                                                quiz.questions.forEach { q ->
                                                                    if (!orderedQuestions.any { it.id == q.id }) {
                                                                        orderedQuestions.add(q)
                                                                    }
                                                                }
                                                                // Validate currentQuestionIndex is within bounds
                                                                val safeIndex = if (progress.currentQuestionIndex < orderedQuestions.size) {
                                                                    progress.currentQuestionIndex
                                                                } else {
                                                                    0 // Reset to start if index is out of bounds
                                                                }
                                                                currentQuiz = quiz.copy(questions = orderedQuestions)
                                                                navController.navigate("question/$safeIndex/${progress.score}")
                                                            }
                                                        }
                                                    } else {
                                                        // New quiz - shuffle questions
                                                        val shuffledQuestions = quiz.questions.shuffled().toMutableList()
                                                        currentQuiz = quiz.copy(questions = shuffledQuestions)
                                                        navController.navigate("question/0/0")
                                                    }
                                                }
                                            } else {
                                                // No userId - just shuffle and start
                                                val shuffledQuestions = quiz.questions.shuffled().toMutableList()
                                                currentQuiz = quiz.copy(questions = shuffledQuestions)
                                                navController.navigate("question/0/0")
                                            }
                                        } else {
                                            Toast.makeText(context, "No quiz available for $difficulty", Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure {
                                        Toast.makeText(context, "Failed to load quiz: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                }

                composable("question/{startIndex}/{startScore}") { backEntry ->
                    val quiz = currentQuiz
                    val startIndex = backEntry.arguments?.getString("startIndex")?.toIntOrNull() ?: 0
                    val startScore = backEntry.arguments?.getString("startScore")?.toIntOrNull() ?: 0
                    if (quiz != null && quiz.questions.isNotEmpty()) {
                        QuizQuestionScreen(
                            quiz = quiz,
                            difficulty = selectedDifficulty,
                            userEmail = userEmail,
                            userRole = userRole,
                            userQuizRepository = userQuizRepository,
                            startIndex = startIndex,
                            startScore = startScore,
                            onComplete = { score, totalQuestions, answers ->
                                // Save quiz attempt and delete progress
                                CoroutineScope(Dispatchers.IO).launch {
                                    val userId = getUserIdFromEmail(userEmail, userRole)
                                    if (userId != null && quiz.id.isNotEmpty()) {
                                        // Delete progress since quiz is completed
                                        userQuizRepository.deleteQuizProgress(userId, selectedDifficulty)
                                        
                                        val percentage = (score.toDouble() / totalQuestions.toDouble()) * 100.0
                                        val attempt = QuizAttempt(
                                            userId = userId,
                                            userEmail = userEmail,
                                            quizId = quiz.id,
                                            difficulty = selectedDifficulty.uppercase(),
                                            score = score,
                                            totalQuestions = totalQuestions,
                                            percentage = percentage,
                                            answers = answers
                                        )
                                        userQuizRepository.saveQuizAttempt(attempt)
                                        
                                        // Refresh best score
                                        val bestResult = userQuizRepository.getUserBestScore(userId, selectedDifficulty)
                                        var isPerfect = false
                                        withContext(Dispatchers.Main) {
                                            bestResult.onSuccess { bestAttempt ->
                                                userBestScores = userBestScores + (selectedDifficulty to bestAttempt)
                                                isPerfect = bestAttempt != null && (bestAttempt.percentage ?: 0.0) >= 100.0
                                            }
                                        }
                                        
                                        // Create notification for quiz completion
                                        val notificationRepository = com.example.expressora.backend.NotificationRepository()
                                        val difficultyDisplay = when (selectedDifficulty.uppercase()) {
                                            "EASY" -> "Easy"
                                            "MEDIUM" -> "Medium"
                                            "DIFFICULT" -> "Difficult"
                                            "PRO" -> "Pro"
                                            else -> selectedDifficulty
                                        }
                                        
                                        // Check if all levels are perfect
                                        val allLevelsPerfect = withContext(Dispatchers.IO) {
                                            val difficulties = listOf("Easy", "Medium", "Difficult", "Pro")
                                            val allAttempts = difficulties.mapNotNull { diff ->
                                                userQuizRepository.getUserBestScore(userId, diff).getOrNull()
                                            }
                                            allAttempts.size == 4 && allAttempts.all { (it.percentage ?: 0.0) >= 100.0 }
                                        }
                                        
                                        if (isPerfect) {
                                            // Perfect score notification
                                            val perfectNotification = com.example.expressora.models.Notification(
                                                userId = userId,
                                                userEmail = userEmail,
                                                title = "Perfect Score! 🎉",
                                                message = "Congratulations! You got a perfect score in $difficultyDisplay level! Next level unlocked!",
                                                type = "quiz_perfect",
                                                isRead = false,
                                                createdAt = java.util.Date()
                                            )
                                            notificationRepository.createNotification(perfectNotification)
                                            
                                            // Check if all levels are perfect
                                            if (allLevelsPerfect) {
                                                val allPerfectNotification = com.example.expressora.models.Notification(
                                                    userId = userId,
                                                    userEmail = userEmail,
                                                    title = "All Levels Perfect! 🏆",
                                                    message = "Amazing! You've completed all quiz levels perfectly! Wait for new questions to be added.",
                                                    type = "quiz_all_perfect",
                                                    isRead = false,
                                                    createdAt = java.util.Date()
                                                )
                                                notificationRepository.createNotification(allPerfectNotification)
                                            }
                                        } else {
                                            // Regular completion notification
                                            val notification = com.example.expressora.models.Notification(
                                                userId = userId,
                                                userEmail = userEmail,
                                                title = "Achievement",
                                                message = "You completed a quiz in $difficultyDisplay level! Get a perfect score to unlock the next level.",
                                                type = "quiz_completion",
                                                isRead = false,
                                                createdAt = java.util.Date()
                                            )
                                            val notificationResult = notificationRepository.createNotification(notification)
                                            if (notificationResult.isFailure) {
                                                android.util.Log.e("Quiz", "Failed to create notification: ${notificationResult.exceptionOrNull()?.message}")
                                            }
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        navController.navigate("completion/$score/$totalQuestions")
                                    }
                                }
                            })
                    }
                }

                composable("completion/{score}/{total}") { backStackEntry ->
                    val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
                    val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 10
                    QuizCompletionScreen(
                        score = score,
                        totalQuestions = total,
                        onNextCourse = {
                            navController.popBackStack("difficulty", inclusive = false)
                        })
                }
            }
        }
    }
}

@Composable
fun QuizDifficultyScreen(
    selectedDifficulty: String,
    userBestScores: Map<String, QuizAttempt?>,
    userQuizRepository: UserQuizRepository,
    onDifficultySelected: (String) -> Unit
) {
    val difficulties = listOf("Easy", "Medium", "Difficult", "Pro")
    
    // Helper function to check if level is unlocked
    fun isLevelUnlocked(difficulty: String): Boolean {
        return when (difficulty) {
            "Easy" -> true // Easy is always unlocked
            "Medium" -> {
                val easyAttempt = userBestScores["Easy"]
                easyAttempt != null && (easyAttempt.percentage ?: 0.0) >= 100.0
            }
            "Difficult" -> {
                val mediumAttempt = userBestScores["Medium"]
                mediumAttempt != null && (mediumAttempt.percentage ?: 0.0) >= 100.0
            }
            "Pro" -> {
                val difficultAttempt = userBestScores["Difficult"]
                difficultAttempt != null && (difficultAttempt.percentage ?: 0.0) >= 100.0
            }
            else -> false
        }
    }
    
    // Helper function to check if level is perfect (100%)
    fun isLevelPerfect(difficulty: String): Boolean {
        val attempt = userBestScores[difficulty]
        return attempt != null && (attempt.percentage ?: 0.0) >= 100.0
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        Text(
            text = "Quiz",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(16.dp)
        )

        difficulties.forEach { difficulty ->
            val bestAttempt = userBestScores[difficulty]
            val isCompleted = bestAttempt != null
            val bestScore = bestAttempt?.score ?: 0
            val totalQuestions = bestAttempt?.totalQuestions ?: 0
            val isUnlocked = isLevelUnlocked(difficulty)
            val isPerfect = isLevelPerfect(difficulty)
            
            DifficultyRow(
                label = difficulty,
                isCompleted = isCompleted,
                bestScore = if (isCompleted) "$bestScore/$totalQuestions" else null,
                isUnlocked = isUnlocked,
                isPerfect = isPerfect,
                onClick = { 
                    if (isUnlocked && !isPerfect) {
                        onDifficultySelected(difficulty)
                    }
                })
        }
    }
}

@Composable
fun DifficultyRow(
    label: String, 
    isCompleted: Boolean, 
    bestScore: String?, 
    isUnlocked: Boolean = true,
    isPerfect: Boolean = false,
    onClick: () -> Unit
) {
    val bg = when {
        isPerfect -> Color(0xFFFFE082) // Soft warm gold for perfect - matches app's yellow theme
        isCompleted -> Color(0xFFBBFFA0) // Light green for completed
        !isUnlocked -> Color(0xFFE8E8E8) // Soft muted gray for locked - matches app's soft aesthetic
        else -> Color(0xFFF8F8F8) // Default
    }
    
    val textColor = if (!isUnlocked) Color(0xFF999999) else if (isPerfect) Color(0xFFB8860B) else Color.Black
    val isClickable = isUnlocked && !isPerfect
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .then(
                if (isClickable) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (bestScore != null || !isUnlocked || isPerfect) 72.dp else 56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color.Black
                    )
                    if (isPerfect) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✓ Perfect",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = Color(0xFFB8860B) // Dark goldenrod to complement soft gold background
                        )
                    } else if (!isUnlocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔒",
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            color = Color(0xFF999999) // Soft gray to match app's subtitle color
                        )
                    }
                }
                if (bestScore != null) {
                    Text(
                        text = "Best: $bestScore",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        fontFamily = InterFontFamily
                    )
                } else if (!isUnlocked) {
                    Text(
                        text = "Complete previous level perfectly to unlock",
                        fontSize = 14.sp,
                        color = Color(0xFF666666), // Matching lesson list subtitle color
                        fontFamily = InterFontFamily
                    )
                }
            }
            if (isClickable) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Black
                )
            }
        }
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}

// Helper function to get userId from email and role
suspend fun getUserIdFromEmail(email: String, role: String): String? {
    return try {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val snapshot = firestore.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("role", role)
            .get()
            .await()
        if (!snapshot.isEmpty) {
            snapshot.documents[0].id
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun QuizQuestionScreen(
    quiz: Quiz,
    difficulty: String,
    userEmail: String,
    userRole: String,
    userQuizRepository: UserQuizRepository,
    startIndex: Int = 0,
    startScore: Int = 0,
    onComplete: (score: Int, totalQuestions: Int, answers: List<QuizAnswer>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentQuestionIndex by remember { mutableStateOf(startIndex) }
    val totalQuestions = quiz.questions.size
    var score by remember { mutableStateOf(startScore) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showCorrectAnswer by remember { mutableStateOf(false) }
    val userAnswers = remember { mutableStateListOf<QuizAnswer>() }
    
    // Load existing answers and verify score if continuing
    LaunchedEffect(Unit) {
        if (startIndex > 0 || startScore > 0) {
            withContext(Dispatchers.IO) {
                val userId = getUserIdFromEmail(userEmail, userRole)
                if (userId != null) {
                    val progressResult = userQuizRepository.getQuizProgress(userId, difficulty)
                    progressResult.onSuccess { progress ->
                        if (progress != null) {
                            withContext(Dispatchers.Main) {
                                // Load saved answers
                                userAnswers.clear()
                                userAnswers.addAll(progress.answers)
                                // Ensure score matches saved progress (in case of any discrepancy)
                                if (progress.score != startScore) {
                                    score = progress.score
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Save progress when user navigates away (even if they haven't answered current question)
    // Only save if quiz is not completed (currentQuestionIndex < totalQuestions)
    DisposableEffect(currentQuestionIndex, score, userAnswers.size) {
        onDispose {
            // Capture current values at disposal time
            val indexToSave = currentQuestionIndex
            val scoreToSave = score
            val answersToSave = userAnswers.toList()
            
            coroutineScope.launch(Dispatchers.IO) {
                val userId = getUserIdFromEmail(userEmail, userRole)
                if (userId != null && quiz.id.isNotEmpty() && indexToSave < totalQuestions) {
                    val questionOrder = quiz.questions.map { it.id }
                    val progress = QuizProgress(
                        userId = userId,
                        userEmail = userEmail,
                        quizId = quiz.id,
                        difficulty = difficulty.uppercase(),
                        currentQuestionIndex = indexToSave, // Save current index where user is viewing
                        score = scoreToSave,
                        totalQuestions = totalQuestions,
                        answers = answersToSave,
                        questionOrder = questionOrder,
                        updatedAt = java.util.Date()
                    )
                    userQuizRepository.saveQuizProgress(progress)
                }
            }
        }
    }
    
    val currentQuestion = if (currentQuestionIndex < quiz.questions.size) {
        quiz.questions[currentQuestionIndex]
    } else {
        null
    }
    
    // Reset states when question changes
    LaunchedEffect(currentQuestionIndex) {
        selectedAnswer = null
        showCorrectAnswer = false
    }
    
    if (currentQuestion == null) {
        // Quiz completed
        LaunchedEffect(Unit) {
            onComplete(score, totalQuestions, userAnswers.toList())
        }
        return
    }
    
    // Create answer options (correct answer + wrong options, shuffled)
    val allAnswers = remember(currentQuestion) {
        (listOf(currentQuestion.correctAnswer) + currentQuestion.wrongOptions).shuffled()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = difficulty,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${currentQuestionIndex + 1}/$totalQuestions",
                    fontSize = 16.sp,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF666666)
                )
            }
            Text(
                text = "Score: $score",
                fontSize = 16.sp,
                fontFamily = InterFontFamily,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = currentQuestion.text.ifBlank { "What sign is this?" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color(0xFFF2F4F7), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentQuestion.imageUri != null && currentQuestion.imageUri.toString().isNotEmpty()) {
                        val context = LocalContext.current
                        val imageData = if (currentQuestion.imageUri.toString().startsWith("data:image")) {
                            currentQuestion.imageUri.toString()
                        } else {
                            currentQuestion.imageUri
                        }
                        
                        // Try to decode as bitmap first for better visibility
                        val bitmap = remember(imageData, context) {
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
                                    val inputStream = context.contentResolver.openInputStream(imageData as android.net.Uri)
                                    inputStream?.use { BitmapFactory.decodeStream(it) }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                        
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Quiz Sign",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Fallback to AsyncImage if bitmap decoding fails
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageData)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Quiz Sign",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Text("No image", fontFamily = InterFontFamily, color = Color.Gray)
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allAnswers.chunked(2).forEach { rowAnswers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowAnswers.forEach { answer ->
                                val isSelected = selectedAnswer == answer
                                val isCorrectAnswer = answer == currentQuestion.correctAnswer
                                val showAsCorrect = showCorrectAnswer && isCorrectAnswer && !isSelected
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .background(
                                            when {
                                                showAsCorrect -> Color(0xFFBBFFA0) // Light green for correct answer when wrong was selected (same as selected)
                                                isSelected && isCorrectAnswer -> Color(0xFFBBFFA0) // Light green for correct selected
                                                isSelected && !isCorrectAnswer -> Color(0xFFFFCCCB) // Light red for wrong selected
                                                else -> Color(0xFFFDE58A) // Yellow for unselected
                                            },
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }) {
                                            if (selectedAnswer == null) {
                                                selectedAnswer = answer
                                                val wasCorrect = answer == currentQuestion.correctAnswer
                                                if (wasCorrect) {
                                                    score++
                                                } else {
                                                    // Show correct answer if wrong answer was selected
                                                    showCorrectAnswer = true
                                                }
                                                
                                                // Save answer
                                                userAnswers.add(
                                                    QuizAnswer(
                                                        questionId = currentQuestion.id,
                                                        selectedAnswer = answer,
                                                        isCorrect = wasCorrect
                                                    )
                                                )
                                            }
                                        }, 
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = answer,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = InterFontFamily
                                    )
                                }

                                        if (isSelected) {
                                            LaunchedEffect(answer) {
                                                delay(2000) // Show result for 2 seconds (to see correct answer)
                                                
                                                selectedAnswer = null
                                                showCorrectAnswer = false
                                                if (currentQuestionIndex < totalQuestions - 1) {
                                                    // Move to next question
                                                    currentQuestionIndex++
                                                    // Save progress after moving to next question
                                                    // Save the NEW current index (the question user is now viewing)
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        val userId = getUserIdFromEmail(userEmail, userRole)
                                                        if (userId != null && quiz.id.isNotEmpty()) {
                                                            val questionOrder = quiz.questions.map { it.id }
                                                            val progress = QuizProgress(
                                                                userId = userId,
                                                                userEmail = userEmail,
                                                                quizId = quiz.id,
                                                                difficulty = difficulty.uppercase(),
                                                                currentQuestionIndex = currentQuestionIndex, // Save the question user is now viewing
                                                                score = score,
                                                                totalQuestions = totalQuestions,
                                                                answers = userAnswers.toList(),
                                                                questionOrder = questionOrder,
                                                                updatedAt = java.util.Date()
                                                            )
                                                            userQuizRepository.saveQuizProgress(progress)
                                                        }
                                                    }
                                                } else {
                                                    onComplete(score, totalQuestions, userAnswers.toList())
                                                }
                                            }
                                        }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizCompletionScreen(
    score: Int,
    totalQuestions: Int,
    onNextCourse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "$score/$totalQuestions",
            fontSize = 35.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            color = Color(0xFFFFC107)
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                "Next Level",
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

@Preview(showBackground = true)
@Composable
fun PreviewQuizApp() {
    QuizApp()
}
