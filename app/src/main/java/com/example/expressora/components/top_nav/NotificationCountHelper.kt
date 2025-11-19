package com.example.expressora.components.top_nav

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.expressora.backend.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Helper function to get userId from email and role
suspend fun getUserIdFromEmailForNotification(email: String, role: String): String? {
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
fun rememberNotificationCount(userEmail: String, userRole: String): Int {
    var notificationCount by remember { mutableStateOf(0) }
    val notificationRepository = remember { NotificationRepository() }
    var refreshKey by remember { mutableStateOf(0) }

    // Load notification count
    fun loadNotificationCount() {
        if (userEmail.isNotEmpty()) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userId = getUserIdFromEmailForNotification(userEmail, userRole)
                    if (userId != null) {
                        android.util.Log.d("NotificationCount", "Loading notifications for userId: $userId")
                        val result = notificationRepository.getUserNotifications(userId)
                        result.onSuccess { notifications ->
                            val unreadCount = notifications.count { !it.isRead }
                            android.util.Log.d("NotificationCount", "Found $unreadCount unread notifications out of ${notifications.size} total")
                            withContext(Dispatchers.Main) {
                                notificationCount = unreadCount
                            }
                        }.onFailure {
                            android.util.Log.e("NotificationCount", "Failed to load notifications: ${it.message}", it)
                        }
                    } else {
                        android.util.Log.w("NotificationCount", "userId is null for email: $userEmail, role: $userRole")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationCount", "Exception loading notifications: ${e.message}", e)
                }
            }
        }
    }

    // Load on initial composition and when userEmail/userRole changes
    LaunchedEffect(userEmail, userRole) {
        loadNotificationCount()
    }

    // Also refresh when screen becomes visible (using refreshKey)
    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            loadNotificationCount()
        }
    }

    // Periodic refresh every 5 seconds when screen is visible
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // Refresh every 5 seconds
            loadNotificationCount()
        }
    }

    // Expose refresh function (can be called from outside)
    DisposableEffect(Unit) {
        onDispose {
            // Refresh when screen is disposed (user navigated away)
            refreshKey++
        }
    }

    return notificationCount
}

