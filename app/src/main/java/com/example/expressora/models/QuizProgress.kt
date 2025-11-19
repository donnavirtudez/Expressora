package com.example.expressora.models

import java.util.Date

data class QuizProgress(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val quizId: String = "",
    val difficulty: String = "",
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val answers: List<QuizAnswer> = emptyList(),
    val questionOrder: List<String> = emptyList(), // Store question IDs in shuffled order
    val updatedAt: Date = Date()
)

