package com.example.expressora.backend

import com.example.expressora.dashboard.admin.quizmanagement.Difficulty
import com.example.expressora.dashboard.admin.quizmanagement.Question
import com.example.expressora.dashboard.admin.quizmanagement.Quiz
import com.example.expressora.models.QuizAnswer
import com.example.expressora.models.QuizAttempt
import com.example.expressora.models.QuizProgress
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

class UserQuizRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val quizzesCollection = firestore.collection("quizzes")
    private val attemptsCollection = firestore.collection("quiz_attempts")
    private val quizProgressCollection = firestore.collection("quiz_progress")

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
                        ?: System.currentTimeMillis()

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

    suspend fun getQuizByDifficulty(difficulty: String): Result<Quiz?> {
        return try {
            val difficultyEnum = Difficulty.valueOf(difficulty.uppercase())
            val snapshot = quizzesCollection
                .whereEqualTo("difficulty", difficultyEnum.name)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
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
                    ?: System.currentTimeMillis()

                val quiz = Quiz(
                    id = doc.id,
                    difficulty = difficultyEnum,
                    questions = questions.toMutableList(),
                    lastUpdated = updatedAt
                )
                Result.success(quiz)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveQuizAttempt(attempt: QuizAttempt): Result<String> {
        return try {
            val attemptMap = hashMapOf<String, Any>(
                "userId" to attempt.userId,
                "userEmail" to attempt.userEmail,
                "quizId" to attempt.quizId,
                "difficulty" to attempt.difficulty,
                "score" to attempt.score,
                "totalQuestions" to attempt.totalQuestions,
                "percentage" to attempt.percentage,
                "completedAt" to attempt.completedAt,
                "answers" to attempt.answers.map { answer ->
                    hashMapOf<String, Any>(
                        "questionId" to answer.questionId,
                        "selectedAnswer" to answer.selectedAnswer,
                        "isCorrect" to answer.isCorrect
                    )
                }
            )

            val docRef = attemptsCollection.document()
            docRef.set(attemptMap).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserAttempts(userId: String, difficulty: String): Result<List<QuizAttempt>> {
        return try {
            val snapshot = attemptsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("difficulty", difficulty.uppercase())
                .get()
                .await()

            val attempts = snapshot.documents.mapNotNull { doc ->
                try {
                    val answersData = doc.get("answers") as? List<Map<String, Any>> ?: emptyList()
                    val answers = answersData.mapNotNull { aMap ->
                        try {
                            QuizAnswer(
                                questionId = aMap["questionId"] as? String ?: "",
                                selectedAnswer = aMap["selectedAnswer"] as? String ?: "",
                                isCorrect = aMap["isCorrect"] as? Boolean ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val completedAt = (doc.get("completedAt") as? com.google.firebase.Timestamp)?.toDate()
                        ?: Date()

                    QuizAttempt(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        quizId = doc.getString("quizId") ?: "",
                        difficulty = doc.getString("difficulty") ?: "",
                        score = (doc.get("score") as? Long)?.toInt() ?: 0,
                        totalQuestions = (doc.get("totalQuestions") as? Long)?.toInt() ?: 0,
                        percentage = (doc.get("percentage") as? Double) ?: 0.0,
                        completedAt = completedAt,
                        answers = answers
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(attempts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserBestScore(userId: String, difficulty: String): Result<QuizAttempt?> {
        return try {
            val attemptsResult = getUserAttempts(userId, difficulty)
            if (attemptsResult.isFailure) {
                return attemptsResult.map { null }
            }

            val attempts = attemptsResult.getOrNull() ?: return Result.success(null)
            val bestAttempt = attempts.maxByOrNull { it.percentage }
            Result.success(bestAttempt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveQuizProgress(progress: QuizProgress): Result<String> {
        return try {
            val progressMap = hashMapOf<String, Any>(
                "userId" to progress.userId,
                "userEmail" to progress.userEmail,
                "quizId" to progress.quizId,
                "difficulty" to progress.difficulty,
                "currentQuestionIndex" to progress.currentQuestionIndex,
                "score" to progress.score,
                "totalQuestions" to progress.totalQuestions,
                "questionOrder" to progress.questionOrder,
                "updatedAt" to progress.updatedAt,
                "answers" to progress.answers.map { answer ->
                    hashMapOf<String, Any>(
                        "questionId" to answer.questionId,
                        "selectedAnswer" to answer.selectedAnswer,
                        "isCorrect" to answer.isCorrect
                    )
                }
            )

            // Check if progress already exists
            val existingProgress = quizProgressCollection
                .whereEqualTo("userId", progress.userId)
                .whereEqualTo("difficulty", progress.difficulty.uppercase())
                .get()
                .await()

            if (existingProgress.isEmpty) {
                // Create new progress
                val docRef = quizProgressCollection.document()
                progressMap["id"] = docRef.id
                docRef.set(progressMap).await()
                Result.success(docRef.id)
            } else {
                // Update existing progress
                val docId = existingProgress.documents[0].id
                quizProgressCollection.document(docId).set(progressMap, SetOptions.merge()).await()
                Result.success(docId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuizProgress(userId: String, difficulty: String): Result<QuizProgress?> {
        return try {
            val snapshot = quizProgressCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("difficulty", difficulty.uppercase())
                .get()
                .await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val answersData = doc.get("answers") as? List<Map<String, Any>> ?: emptyList()
                val answers = answersData.mapNotNull { aMap ->
                    try {
                        QuizAnswer(
                            questionId = aMap["questionId"] as? String ?: "",
                            selectedAnswer = aMap["selectedAnswer"] as? String ?: "",
                            isCorrect = aMap["isCorrect"] as? Boolean ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                val questionOrder = (doc.get("questionOrder") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val updatedAt = (doc.get("updatedAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date()

                val progress = QuizProgress(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userEmail = doc.getString("userEmail") ?: "",
                    quizId = doc.getString("quizId") ?: "",
                    difficulty = doc.getString("difficulty") ?: "",
                    currentQuestionIndex = (doc.get("currentQuestionIndex") as? Long)?.toInt() ?: 0,
                    score = (doc.get("score") as? Long)?.toInt() ?: 0,
                    totalQuestions = (doc.get("totalQuestions") as? Long)?.toInt() ?: 0,
                    answers = answers,
                    questionOrder = questionOrder,
                    updatedAt = updatedAt
                )
                Result.success(progress)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuizProgress(userId: String, difficulty: String): Result<Unit> {
        return try {
            val snapshot = quizProgressCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("difficulty", difficulty.uppercase())
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

