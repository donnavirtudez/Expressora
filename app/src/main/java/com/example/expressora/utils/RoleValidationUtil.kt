package com.example.expressora.utils

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.example.expressora.auth.LoginActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

/**
 * Utility class for role-based access control
 * Ensures users can only access activities/screens for their role
 */
object RoleValidationUtil {
    
    /**
     * Validates if the current user has the required role and redirects to login if not
     * This function should be called in onCreate() of Activities before setContent()
     * 
     * @param activity The ComponentActivity to validate
     * @param requiredRole The role required to access this activity ("user" or "admin")
     * @param onValidated Callback when validation is complete (true if valid, false if invalid)
     */
    fun validateRoleAndRedirect(
        activity: ComponentActivity,
        requiredRole: String,
        onValidated: (Boolean) -> Unit
    ) {
        val sharedPref = activity.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", null)
        val storedRole = sharedPref.getString("user_role", null)
        
        // If no session exists, redirect to login
        if (userEmail == null || storedRole == null) {
            redirectToLogin(activity)
            onValidated(false)
            return
        }
        
        // If stored role doesn't match required role, redirect to login
        if (storedRole != requiredRole) {
            redirectToLogin(activity)
            onValidated(false)
            return
        }
        
        // Verify role in Firestore to ensure security
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .whereEqualTo("email", userEmail)
            .whereEqualTo("role", requiredRole)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // User doesn't exist with this role in Firestore, redirect to login
                    redirectToLogin(activity)
                    onValidated(false)
                } else {
                    // Validation passed
                    onValidated(true)
                }
            }
            .addOnFailureListener {
                // Error checking Firestore, redirect to login for security
                redirectToLogin(activity)
                onValidated(false)
            }
    }
    
    /**
     * Redirects user to login screen and clears session
     */
    private fun redirectToLogin(activity: ComponentActivity) {
        // Clear Firebase Auth
        FirebaseAuth.getInstance().signOut()
        
        // Clear SharedPreferences
        val sharedPref = activity.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
        
        // Redirect to login
        val intent = Intent(activity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish()
    }
    
    /**
     * Quick check if current user has the required role (synchronous check from SharedPreferences only)
     * Use this for UI decisions, but always validate with validateRoleAndRedirect in onCreate
     */
    fun hasRole(context: Context, requiredRole: String): Boolean {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val storedRole = sharedPref.getString("user_role", null)
        return storedRole == requiredRole
    }
}

