package com.example.expressora.models

import java.util.Date

data class QuizAnswer(
    val questionId: String,
    val selectedAnswer: String,
    val isCorrect: Boolean
)

data class QuizAttempt(
    val id: String = "",
    val userId: String,
    val userEmail: String,
    val quizId: String,
    val difficulty: String,
    val score: Int,
    val totalQuestions: Int,
    val percentage: Double,
    val completedAt: Date = Date(),
    val answers: List<QuizAnswer> = emptyList()
)

