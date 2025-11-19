package com.example.expressora.backend

import com.example.expressora.dashboard.admin.quizmanagement.Difficulty
import com.example.expressora.dashboard.admin.quizmanagement.Question
import com.example.expressora.dashboard.admin.quizmanagement.Quiz
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class QuizRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val quizzesCollection = firestore.collection("quizzes")

    suspend fun saveQuiz(quiz: Quiz, adminEmail: String): Result<String> {
        return try {
            val quizMap = hashMapOf<String, Any>(
                "difficulty" to quiz.difficulty.name,
                "createdBy" to adminEmail,
                "questions" to quiz.questions.map { question ->
                    hashMapOf<String, Any>(
                        "id" to question.id,
                        "text" to question.text,
                        "imageUri" to (question.imageUri?.toString() ?: ""),
                        "correctAnswer" to question.correctAnswer,
                        "wrongOptions" to question.wrongOptions
                    )
                }
            )

            // Check if quiz already exists (by difficulty)
            val existingQuiz = quizzesCollection
                .whereEqualTo("difficulty", quiz.difficulty.name)
                .get()
                .await()

            if (existingQuiz.isEmpty) {
                // New quiz - add createdAt, no updatedAt
                quizMap["createdAt"] = Date()
                val docRef = if (quiz.id.isNotEmpty() && quiz.id.length > 20) {
                    // Use existing ID if it's a valid Firestore ID
                    quizzesCollection.document(quiz.id)
                } else {
                    // Generate new document
                    quizzesCollection.document()
                }
                docRef.set(quizMap).await()
                
                // Notify all users that a new quiz is available (in background, don't block)
                if (quiz.questions.isNotEmpty()) {
                    // Use GlobalScope to launch notification creation in background
                    // This won't block the quiz creation
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val notificationRepository = com.example.expressora.backend.NotificationRepository()
                            val difficultyDisplay = when (quiz.difficulty) {
                                Difficulty.EASY -> "Easy"
                                Difficulty.MEDIUM -> "Medium"
                                Difficulty.DIFFICULT -> "Difficult"
                                Difficulty.PRO -> "Pro"
                            }
                            val message = "A new quiz has been CREATED in $difficultyDisplay level! Take it now and test your knowledge."
                            val result = notificationRepository.createNotificationForAllUsers(
                                title = "Quiz Created - New Quiz Available",
                                message = message,
                                type = "quiz_created"
                            )
                            result.onSuccess { count ->
                                android.util.Log.d("QuizRepository", "Created $count notifications for new quiz")
                            }.onFailure { e ->
                                android.util.Log.e("QuizRepository", "Failed to create notifications: ${e.message}", e)
                            }
                        } catch (e: Exception) {
                            // Don't fail quiz creation if notification fails
                            android.util.Log.e("QuizRepository", "Exception creating notifications: ${e.message}", e)
                        }
                    }
                }
                
                Result.success(docRef.id)
            } else {
                // Update existing quiz - only set updatedAt if quiz.lastUpdated > 0
                if (quiz.lastUpdated > 0) {
                    // Get old quiz data to compare question counts and IDs
                    val oldQuizDoc = existingQuiz.documents[0]
                    val oldQuestionsData = oldQuizDoc.get("questions") as? List<Map<String, Any>> ?: emptyList()
                    val oldQuestionIds = oldQuestionsData.mapNotNull { it["id"] as? String }.toSet()
                    val oldQuestionCount = oldQuestionIds.size
                    val newQuestionIds = quiz.questions.map { it.id }.toSet()
                    val newQuestionCount = newQuestionIds.size
                    
                    // Determine what type of change happened
                    val questionsChanged = oldQuestionIds != newQuestionIds || oldQuestionCount != newQuestionCount
                    val questionsWereAdded = newQuestionCount > oldQuestionCount
                    val questionsWereRemoved = oldQuestionCount > newQuestionCount
                    val questionsWereModified = oldQuestionCount == newQuestionCount && oldQuestionIds != newQuestionIds
                    
                    // Check if ONLY questions were added (no removals or modifications)
                    // If new IDs contain all old IDs, it means only additions happened
                    val onlyQuestionsAdded = questionsWereAdded && !questionsWereRemoved && !questionsWereModified && 
                                            oldQuestionIds.all { it in newQuestionIds }
                    
                    // Check if only text/content changed (same IDs, same count, but content might differ)
                    // This is considered a minor change - no notification needed
                    val onlyTextChanged = oldQuestionIds == newQuestionIds && oldQuestionCount == newQuestionCount
                    
                    quizMap["updatedAt"] = Date()
                    val docId = existingQuiz.documents[0].id
                    quizzesCollection.document(docId).set(quizMap, SetOptions.merge()).await()
                    
                    // Only notify for SIGNIFICANT changes (Option B: Significant changes only)
                    // Significant = questions added/removed/modified (structure changed)
                    // Not significant = only text/content changed (same question IDs)
                    val isSignificantChange = questionsChanged && !onlyTextChanged
                    
                    // If quiz was actually updated with significant changes, handle based on change type
                    if (isSignificantChange) {
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                val firestore = FirebaseFirestore.getInstance()
                                val attemptsCollection = firestore.collection("quiz_attempts")
                                val progressCollection = firestore.collection("quiz_progress")
                                
                                // Get all attempts for this difficulty
                                val attemptsSnapshot = attemptsCollection
                                    .whereEqualTo("difficulty", quiz.difficulty.name.uppercase())
                                    .get()
                                    .await()
                                
                                // Get unique user IDs who have completed this quiz
                                val userIdsWithAttempts = attemptsSnapshot.documents
                                    .mapNotNull { it.getString("userId") }
                                    .distinct()
                                
                                // Always reset best scores when quiz changes
                                attemptsSnapshot.documents.forEach { doc ->
                                    doc.reference.delete().await()
                                }
                                
                                // Reset progress only if questions were removed or modified (not if only added)
                                if (!onlyQuestionsAdded) {
                                    // Questions were removed or modified - reset progress too
                                    val progressSnapshot = progressCollection
                                        .whereEqualTo("difficulty", quiz.difficulty.name.uppercase())
                                        .get()
                                        .await()
                                    
                                    progressSnapshot.documents.forEach { doc ->
                                        doc.reference.delete().await()
                                    }
                                    
                                    android.util.Log.d("QuizRepository", "Deleted ${attemptsSnapshot.documents.size} attempts and ${progressSnapshot.documents.size} progress records - questions removed/modified")
                                } else {
                                    // Only questions added - preserve progress
                                    android.util.Log.d("QuizRepository", "Deleted ${attemptsSnapshot.documents.size} attempts but preserved progress - only questions added")
                                }
                                
                                // Notify users who had completed the quiz
                                if (userIdsWithAttempts.isNotEmpty()) {
                                    val notificationRepository = com.example.expressora.backend.NotificationRepository()
                                    val difficultyDisplay = when (quiz.difficulty) {
                                        Difficulty.EASY -> "Easy"
                                        Difficulty.MEDIUM -> "Medium"
                                        Difficulty.DIFFICULT -> "Difficult"
                                        Difficulty.PRO -> "Pro"
                                    }
                                    val message = if (onlyQuestionsAdded) {
                                        "Quiz in $difficultyDisplay level has been updated with new questions ($oldQuestionCount → $newQuestionCount). Your best score has been reset, but you can continue from where you left off!"
                                    } else if (questionsWereRemoved) {
                                        "Quiz in $difficultyDisplay level has been updated ($oldQuestionCount → $newQuestionCount questions). Your progress and best score have been reset. Please start fresh!"
                                    } else {
                                        "Quiz in $difficultyDisplay level has been updated. Your progress and best score have been reset. Please start fresh!"
                                    }
                                    
                                    // Create notification for each user who had attempts
                                    userIdsWithAttempts.forEach { userId ->
                                        try {
                                            val userDoc = firestore.collection("users").document(userId).get().await()
                                            val userEmail = userDoc.getString("email") ?: ""
                                            if (userEmail.isNotEmpty()) {
                                                val notification = com.example.expressora.models.Notification(
                                                    userId = userId,
                                                    userEmail = userEmail,
                                                    title = "Quiz Updated - Best Score Reset",
                                                    message = message,
                                                    type = "quiz_updated",
                                                    isRead = false,
                                                    createdAt = Date()
                                                )
                                                notificationRepository.createNotification(notification)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("QuizRepository", "Failed to notify user $userId: ${e.message}", e)
                                        }
                                    }
                                }
                                
                                // Also notify all users about the update
                                val notificationRepository = com.example.expressora.backend.NotificationRepository()
                                val difficultyDisplay = when (quiz.difficulty) {
                                    Difficulty.EASY -> "Easy"
                                    Difficulty.MEDIUM -> "Medium"
                                    Difficulty.DIFFICULT -> "Difficult"
                                    Difficulty.PRO -> "Pro"
                                }
                                val message = if (onlyQuestionsAdded) {
                                    "Quiz in $difficultyDisplay level has been updated with new questions ($oldQuestionCount → $newQuestionCount questions). Check it out now!"
                                } else if (questionsWereRemoved) {
                                    "Quiz in $difficultyDisplay level has been updated ($oldQuestionCount → $newQuestionCount questions). Check it out now!"
                                } else {
                                    "Quiz in $difficultyDisplay level has been updated. Check it out now!"
                                }
                                val result = notificationRepository.createNotificationForAllUsers(
                                    title = "Quiz Updated - Changes Made",
                                    message = message,
                                    type = "quiz_updated"
                                )
                                result.onSuccess { count ->
                                    android.util.Log.d("QuizRepository", "Created $count notifications for quiz update")
                                }.onFailure { e ->
                                    android.util.Log.e("QuizRepository", "Failed to create update notifications: ${e.message}", e)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("QuizRepository", "Exception handling quiz update: ${e.message}", e)
                            }
                        }
                    } else if (onlyTextChanged) {
                        // Only text/content changed (not significant) - no notification, no reset
                        // Quiz data was saved but only minor text edits were made
                        android.util.Log.d("QuizRepository", "Quiz saved with only text changes - no notification, preserving scores and progress")
                    } else {
                        // No actual changes detected - don't reset scores or notify
                        // Quiz data was saved but no meaningful changes were made
                        android.util.Log.d("QuizRepository", "Quiz saved but no changes detected - preserving scores and progress")
                    }
                    
                    Result.success(docId)
                } else {
                    // No update, just return success
                    val docId = existingQuiz.documents[0].id
                    Result.success(docId)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuizzes(): Result<List<Quiz>> {
        return try {
            val snapshot = quizzesCollection.get().await()
            val quizzes = snapshot.documents.mapNotNull { doc ->
                try {
                    val difficultyStr = doc.getString("difficulty") ?: return@mapNotNull null
                    val difficulty = Difficulty.valueOf(difficultyStr)
                    val questionsData = doc.get("questions") as? List<Map<String, Any>> ?: emptyList()
                    
                    val questions = questionsData.mapNotNull { qMap ->
                        try {
                            Question(
                                id = qMap["id"] as? String ?: "",
                                text = qMap["text"] as? String ?: "",
                                imageUri = android.net.Uri.parse(qMap["imageUri"] as? String ?: "").takeIf { it.toString().isNotEmpty() },
                                correctAnswer = qMap["correctAnswer"] as? String ?: "",
                                wrongOptions = (qMap["wrongOptions"] as? List<*>?)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val updatedAt = (doc.get("updatedAt") as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: 0L // 0 means not updated yet

                    Quiz(
                        id = doc.id,
                        difficulty = difficulty,
                        questions = questions.toMutableList(),
                        lastUpdated = updatedAt
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(quizzes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuizByDifficulty(difficulty: Difficulty): Result<Quiz?> {
        return try {
            val snapshot = quizzesCollection
                .whereEqualTo("difficulty", difficulty.name)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val difficultyStr = doc.getString("difficulty") ?: return Result.success(null)
                val questionsData = doc.get("questions") as? List<Map<String, Any>> ?: emptyList()
                
                val questions = questionsData.mapNotNull { qMap ->
                    try {
                        Question(
                            id = qMap["id"] as? String ?: "",
                            text = qMap["text"] as? String ?: "",
                            imageUri = android.net.Uri.parse(qMap["imageUri"] as? String ?: "").takeIf { it.toString().isNotEmpty() },
                            correctAnswer = qMap["correctAnswer"] as? String ?: "",
                            wrongOptions = (qMap["wrongOptions"] as? List<*>?)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                val updatedAt = (doc.get("updatedAt") as? com.google.firebase.Timestamp)?.toDate()?.time
                    ?: 0L // 0 means not updated yet

                val quiz = Quiz(
                    id = doc.id,
                    difficulty = difficulty,
                    questions = questions.toMutableList(),
                    lastUpdated = updatedAt
                )
                Result.success(quiz)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuiz(quizId: String): Result<Unit> {
        return try {
            // Get quiz data before deleting to know which difficulty was deleted
            val quizDoc = quizzesCollection.document(quizId).get().await()
            val difficultyStr = quizDoc.getString("difficulty") ?: ""
            
            // Delete the quiz
            quizzesCollection.document(quizId).delete().await()
            
            // Delete all quiz attempts and progress for this difficulty (reset best scores and clear progress)
            if (difficultyStr.isNotEmpty()) {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        val attemptsCollection = firestore.collection("quiz_attempts")
                        val progressCollection = firestore.collection("quiz_progress")
                        
                        // Delete all quiz attempts for this difficulty
                        val attemptsSnapshot = attemptsCollection
                            .whereEqualTo("difficulty", difficultyStr.uppercase())
                            .get()
                            .await()
                        attemptsSnapshot.documents.forEach { doc ->
                            doc.reference.delete().await()
                        }
                        
                        // Delete all quiz progress for this difficulty
                        val progressSnapshot = progressCollection
                            .whereEqualTo("difficulty", difficultyStr.uppercase())
                            .get()
                            .await()
                        progressSnapshot.documents.forEach { doc ->
                            doc.reference.delete().await()
                        }
                        
                        android.util.Log.d("QuizRepository", "Deleted quiz: removed ${attemptsSnapshot.documents.size} attempts and ${progressSnapshot.documents.size} progress records for $difficultyStr")
                        
                        // Notify all users that a quiz was deleted
                        val notificationRepository = com.example.expressora.backend.NotificationRepository()
                        val difficultyDisplay = when (difficultyStr.uppercase()) {
                            "EASY" -> "Easy"
                            "MEDIUM" -> "Medium"
                            "DIFFICULT" -> "Difficult"
                            "PRO" -> "Pro"
                            else -> difficultyStr
                        }
                        val message = "The quiz in $difficultyDisplay level has been DELETED and is no longer available. Your progress and best scores have been cleared."
                        val result = notificationRepository.createNotificationForAllUsers(
                            title = "Quiz Deleted - Quiz Removed",
                            message = message,
                            type = "quiz_deleted"
                        )
                        result.onSuccess { count ->
                            android.util.Log.d("QuizRepository", "Created $count notifications for quiz deletion")
                        }.onFailure { e ->
                            android.util.Log.e("QuizRepository", "Failed to create deletion notifications: ${e.message}", e)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("QuizRepository", "Exception handling quiz deletion: ${e.message}", e)
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

