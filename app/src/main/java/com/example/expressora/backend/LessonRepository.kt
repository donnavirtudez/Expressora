package com.example.expressora.backend

import com.example.expressora.dashboard.admin.learningmanagement.Lesson
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import android.net.Uri

class LessonRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val lessonsCollection = firestore.collection("lessons")

    suspend fun saveLesson(lesson: Lesson, adminEmail: String, isNew: Boolean): Result<String> {
        return try {
            val lessonMap = hashMapOf<String, Any>(
                "title" to lesson.title,
                "content" to lesson.content,
                "createdBy" to adminEmail,
                "attachments" to lesson.attachments.map { it.toString() },
                "tryItems" to lesson.tryItems,
                "lastUpdated" to lesson.lastUpdated
            )

            val docRef = if (lesson.id.isNotEmpty() && lesson.id.length > 20) {
                // Use existing ID if it's a valid Firestore ID
                lessonsCollection.document(lesson.id)
            } else {
                // Generate new document
                lessonsCollection.document()
            }

            if (isNew) {
                // New lesson - add createdAt
                lessonMap["createdAt"] = Date()
                docRef.set(lessonMap).await()
                
                // Notify all users that a new lesson is available
                if (lesson.title.isNotEmpty()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val notificationRepository = NotificationRepository()
                            val message = "A new lesson '${lesson.title}' has been CREATED! Check it out now and expand your knowledge."
                            val result = notificationRepository.createNotificationForAllUsers(
                                title = "Lesson Created - New Lesson Available",
                                message = message,
                                type = "lesson_created"
                            )
                            result.onSuccess { count ->
                                android.util.Log.d("LessonRepository", "Created $count notifications for new lesson")
                            }.onFailure { e ->
                                android.util.Log.e("LessonRepository", "Failed to create notifications: ${e.message}", e)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("LessonRepository", "Exception creating notifications: ${e.message}", e)
                        }
                    }
                }
                
                Result.success(docRef.id)
            } else {
                // Update existing lesson - check for significant changes before notifying
                // Get old lesson data to compare
                val oldLessonDoc = docRef.get().await()
                val oldTitle = oldLessonDoc.getString("title") ?: ""
                val oldContent = oldLessonDoc.getString("content") ?: ""
                val oldAttachments = (oldLessonDoc.get("attachments") as? List<String>)?.map { Uri.parse(it) } ?: emptyList()
                val oldTryItems = (oldLessonDoc.get("tryItems") as? List<String>) ?: emptyList()
                
                // Check for significant changes
                val titleChanged = lesson.title.trim() != oldTitle.trim()
                val contentChanged = lesson.content.trim() != oldContent.trim()
                val attachmentsChanged = lesson.attachments != oldAttachments
                val tryItemsChanged = lesson.tryItems != oldTryItems
                
                val isSignificantChange = titleChanged || contentChanged || attachmentsChanged || tryItemsChanged
                
                // Update lesson
                lessonMap["updatedAt"] = Date()
                docRef.set(lessonMap, SetOptions.merge()).await()
                
                // Only notify all users if there are significant changes
                if (isSignificantChange && lesson.title.isNotEmpty()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val notificationRepository = NotificationRepository()
                            val message = "The lesson titled '${lesson.title}' has been updated! Check out the changes."
                            val result = notificationRepository.createNotificationForAllUsers(
                                title = "Lesson Updated",
                                message = message,
                                type = "lesson_updated"
                            )
                            result.onSuccess { count ->
                                android.util.Log.d("LessonRepository", "Created $count notifications for lesson update")
                            }.onFailure { e ->
                                android.util.Log.e("LessonRepository", "Failed to create notifications: ${e.message}", e)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("LessonRepository", "Exception creating notifications: ${e.message}", e)
                        }
                    }
                }
                
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLessons(): Result<List<Lesson>> {
        return try {
            val snapshot = lessonsCollection
                .get()
                .await()

            val lessons = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    if (id.isBlank()) {
                        android.util.Log.w("LessonRepository", "Skipping lesson with blank ID")
                        return@mapNotNull null
                    }

                    val title = doc.getString("title") ?: ""
                    val content = doc.getString("content") ?: ""
                    val attachments = (doc.get("attachments") as? List<*>)?.mapNotNull { uriString ->
                        try {
                            val str = uriString.toString()
                            if (str.isBlank()) null
                            else Uri.parse(str)
                        } catch (e: Exception) {
                            android.util.Log.w("LessonRepository", "Failed to parse attachment URI: $uriString", e)
                            null
                        }
                    } ?: emptyList()
                    
                    val tryItems = (doc.get("tryItems") as? List<*>)?.mapNotNull { 
                        it?.toString() 
                    }?.filter { it.isNotBlank() } ?: emptyList()
                    
                    val lastUpdated = (doc.get("lastUpdated") as? Long) ?: System.currentTimeMillis()

                    Lesson(
                        id = id,
                        title = title,
                        content = content,
                        attachments = attachments,
                        tryItems = tryItems,
                        lastUpdated = lastUpdated
                    )
                } catch (e: Exception) {
                    android.util.Log.e("LessonRepository", "Failed to parse lesson document ${doc.id}", e)
                    null
                }
            }
            
            Result.success(lessons)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLesson(lessonId: String): Result<Unit> {
        return try {
            if (lessonId.isBlank()) {
                return Result.failure(IllegalArgumentException("Lesson ID cannot be blank"))
            }

            // Get lesson title before deleting for notification
            val lessonDoc = lessonsCollection.document(lessonId).get().await()
            val lessonTitle = lessonDoc.getString("title") ?: "Lesson"

            lessonsCollection.document(lessonId).delete().await()
            
            // Notify all users that a lesson has been deleted
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val notificationRepository = NotificationRepository()
                    val message = "The lesson '$lessonTitle' has been DELETED."
                    val result = notificationRepository.createNotificationForAllUsers(
                        title = "Lesson Deleted",
                        message = message,
                        type = "lesson_deleted"
                    )
                    result.onSuccess { count ->
                        android.util.Log.d("LessonRepository", "Created $count notifications for lesson deletion")
                    }.onFailure { e ->
                        android.util.Log.e("LessonRepository", "Failed to create notifications: ${e.message}", e)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LessonRepository", "Exception creating notifications: ${e.message}", e)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
