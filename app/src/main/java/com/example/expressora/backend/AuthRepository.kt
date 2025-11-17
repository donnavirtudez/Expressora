package com.example.expressora.backend

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class AuthRepository {
    private val firestore = FirebaseFirestore.getInstance()

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun loginUser(email: String, password: String): Triple<Boolean, String?, String?> {
        return try {
            val snapshot = firestore.collection("users").whereEqualTo("email", email).get().await()

            if (snapshot.isEmpty) {
                return Triple(false, null, "USER_NOT_FOUND")
            }

            val inputHash = sha256(password)

            // Check all documents with this email to find matching password
            // Since same email + different roles must have different passwords, 
            // there should only be one match
            for (doc in snapshot.documents) {
                val storedHash = doc.getString("password") ?: ""
                val role = doc.getString("role") ?: "user"

                if (storedHash == inputHash) {
                    return Triple(true, role, null)
                }
            }

            Triple(false, null, "INVALID_PASSWORD")
        } catch (e: Exception) {
            Triple(false, null, e.message)
        }
    }
}
