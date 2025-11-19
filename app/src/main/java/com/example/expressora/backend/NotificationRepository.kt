package com.example.expressora.backend

import com.example.expressora.models.Notification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationsCollection = firestore.collection("notifications")

    suspend fun createNotification(notification: Notification): Result<String> {
        return try {
            val notificationMap = hashMapOf<String, Any>(
                "userId" to notification.userId,
                "userEmail" to notification.userEmail,
                "title" to notification.title,
                "message" to notification.message,
                "type" to notification.type,
                "isRead" to notification.isRead,
                "createdAt" to (notification.createdAt ?: Date())
            )

            val docRef = notificationsCollection.document()
            notificationMap["id"] = docRef.id
            docRef.set(notificationMap).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserNotifications(userId: String): Result<List<Notification>> {
        return try {
            // Remove orderBy to avoid requiring a composite index
            // We'll sort in memory instead
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val notifications = snapshot.documents.mapNotNull { doc ->
                try {
                    val createdAt = (doc.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                    Notification(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        type = doc.getString("type") ?: "",
                        isRead = doc.getBoolean("isRead") ?: false,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    null
                }
            }
            // Sort by createdAt descending in memory
            val sortedNotifications = notifications.sortedByDescending { it.createdAt ?: Date(0) }
            Result.success(sortedNotifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all user IDs with role "user"
    suspend fun getAllUserIds(): Result<List<Pair<String, String>>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "user")
                .get()
                .await()

            val userIds = snapshot.documents.mapNotNull { doc ->
                val email = doc.getString("email") ?: ""
                if (email.isNotEmpty()) {
                    Pair(doc.id, email)
                } else {
                    null
                }
            }
            Result.success(userIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Create notification for all users
    suspend fun createNotificationForAllUsers(title: String, message: String, type: String): Result<Int> {
        return try {
            val usersResult = getAllUserIds()
            if (usersResult.isFailure) {
                return Result.failure(usersResult.exceptionOrNull() ?: Exception("Failed to get users"))
            }

            val users = usersResult.getOrNull() ?: return Result.success(0)
            var successCount = 0

            users.forEach { (userId, userEmail) ->
                try {
                    val notification = Notification(
                        userId = userId,
                        userEmail = userEmail,
                        title = title,
                        message = message,
                        type = type,
                        isRead = false,
                        createdAt = Date()
                    )
                    val result = createNotification(notification)
                    if (result.isSuccess) {
                        successCount++
                    }
                } catch (e: Exception) {
                    // Continue with other users even if one fails
                    android.util.Log.e("NotificationRepository", "Failed to create notification for user $userId: ${e.message}")
                }
            }

            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

