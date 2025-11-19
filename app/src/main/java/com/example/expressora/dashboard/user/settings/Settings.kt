package com.example.expressora.dashboard.user.settings

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.expressora.R
import com.example.expressora.auth.LoginActivity
import com.example.expressora.components.top_nav.TopNav
import com.example.expressora.components.top_nav.rememberNotificationCount
import com.example.expressora.components.user_bottom_nav.BottomNav
import com.example.expressora.dashboard.admin.settings.AdminSettingsRow
import com.example.expressora.dashboard.user.community_space.CommunitySpaceActivity
import com.example.expressora.dashboard.user.learn.LearnActivity
import com.example.expressora.dashboard.user.notification.NotificationActivity
import com.example.expressora.dashboard.user.quiz.QuizActivity
import com.example.expressora.dashboard.user.translation.TranslationActivity
import com.example.expressora.ui.theme.InterFontFamily
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Date

private val MutedText = Color(0xFF666666)

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val customSelectionColors = TextSelectionColors(
                handleColor = Color(0xFFFACC15), backgroundColor = Color(0x33FACC15)
            )

            val navController = rememberNavController()

            CompositionLocalProvider(
                LocalTextSelectionColors provides customSelectionColors
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "settings"
                ) {
                    composable("settings") {
                        SettingsScreen(navController)
                    }

                    composable(
                        route = "profile/{label}",
                        arguments = listOf(
                            navArgument("label") {
                                defaultValue = "Personal Information"
                            }
                        )
                    ) { backStackEntry: NavBackStackEntry ->
                        val label =
                            backStackEntry.arguments?.getString("label") ?: "Personal Information"
                        if (label == "Account Information") {
                            AccountInfoScreen(navController, label)
                        } else {
                            UserProfileScreen(navController, label)
                        }
                    }

                    composable(
                        route = "change_email/{label}",
                        arguments = listOf(
                            navArgument("label") {
                                defaultValue = "Change Email"
                            }
                        )
                    ) { backStackEntry: NavBackStackEntry ->
                        val label = backStackEntry.arguments?.getString("label") ?: "Change Email"
                        ChangeEmailScreen(navController, label)
                    }

                    composable(
                        route = "change_password/{label}",
                        arguments = listOf(
                            navArgument("label") {
                                defaultValue = "Change Password"
                            }
                        )
                    ) { backStackEntry: NavBackStackEntry ->
                        val label =
                            backStackEntry.arguments?.getString("label") ?: "Change Password"
                        ChangePasswordScreen(navController, label)
                    }

                    composable(
                        route = "preferences/{label}",
                        arguments = listOf(
                            navArgument("label") {
                                defaultValue = "Preferences"
                            }
                        )
                    ) { backStackEntry: NavBackStackEntry ->
                        val label = backStackEntry.arguments?.getString("label") ?: "Preferences"
                        PreferencesScreen(label)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val settingsItems = listOf(
        "Personal Information", "Account Information", "Preferences", "Log Out"
    )
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userEmail = remember { sharedPref.getString("user_email", "") ?: "" }
    val userRole = remember { sharedPref.getString("user_role", "user") ?: "user" }
    val notificationCount = rememberNotificationCount(userEmail, userRole)
    val logoutDialogVisible = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopNav(
                notificationCount = notificationCount,
                onProfileClick = { /* already in user settings */ },
                onTranslateClick = {
                    context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
                },
                onNotificationClick = {
                    context.startActivity(Intent(context, NotificationActivity::class.java))
                })
        }, bottomBar = {
            BottomNav(onLearnClick = {
                context.startActivity(Intent(context, LearnActivity::class.java))
            }, onCameraClick = {
                context.startActivity(Intent(context, TranslationActivity::class.java))
            }, onQuizClick = {
                context.startActivity(Intent(context, QuizActivity::class.java))
            })
        }, containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Settings",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(16.dp)
            )

            settingsItems.forEach { item ->
                AdminSettingsRow(
                    label = item, showArrow = item != "Log Out", onClick = {
                        when (item) {
                            "Personal Information", "Account Information" -> navController.navigate(
                                "profile/${item}"
                            )

                            "Preferences" -> navController.navigate("preferences/${item}")

                            "Log Out" -> logoutDialogVisible.value = true
                        }
                    })
            }
        }

        if (logoutDialogVisible.value) {
            ConfirmStyledDialog(
                title = "Log Out",
                message = "Are you sure you want to log out?",
                confirmText = "Yes",
                confirmColor = Color(0xFFFACC15),
                confirmTextColor = Color.Black,
                onDismiss = { logoutDialogVisible.value = false },
                onConfirm = {
                    logoutDialogVisible.value = false
                    performLogout(context)
                })
        }
    }
}

@Composable
fun ConfirmStyledDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmTextColor: Color = Color.White
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(message, color = MutedText)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Cancel", color = Color(0xFF666666))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm, colors = ButtonDefaults.buttonColors(
                            containerColor = confirmColor, contentColor = confirmTextColor
                        )
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

fun performLogout(context: Context) {
    FirebaseAuth.getInstance().signOut()
    FirebaseFirestore.getInstance().clearPersistence()

    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        clear()
        apply()
    }

    val intent = Intent(context, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    context.startActivity(intent)
}

// Helper function to hash password
private fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// Function to convert image to base64 string (FREE - no Firebase Storage/billing needed)
// Returns data URI to store in Firestore
suspend fun convertImageToBase64(context: Context, imageUri: Uri): Pair<String?, String?> {
    return try {
        val contentResolver: ContentResolver = context.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(imageUri)

        if (inputStream == null) {
            val errorMsg = "Failed to open input stream from URI: $imageUri"
            android.util.Log.e("Settings", errorMsg)
            return Pair(null, errorMsg)
        }

        inputStream.use { stream: InputStream ->
            // Decode the image
            val bitmap = BitmapFactory.decodeStream(stream)

            if (bitmap == null) {
                val errorMsg = "Failed to decode image"
                android.util.Log.e("Settings", errorMsg)
                return@use Pair<String?, String?>(null, errorMsg)
            }

            // Resize image to reduce size (max 800x800) to fit Firestore 1MB limit
            val maxSize = 800
            val width = bitmap.width
            val height = bitmap.height
            val scale = if (width > height) {
                maxSize.toFloat() / width
            } else {
                maxSize.toFloat() / height
            }

            val resizedBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            // Compress to JPEG (quality 70% to reduce size further)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val imageBytes = outputStream.toByteArray()

            // Convert to base64
            val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64String"

            Pair(dataUri, null)
        }
    } catch (e: Exception) {
        val errorMsg = "Error converting image: ${e.message ?: e.javaClass.simpleName}"
        android.util.Log.e("Settings", errorMsg, e)
        e.printStackTrace()
        Pair(null, errorMsg)
    }
}

// Function to update firstName, lastName, and profile
suspend fun updateUserProfile(
    userId: String,
    firstName: String,
    lastName: String,
    profile: String
): Boolean {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val updates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "profile" to profile
        )
        firestore.collection("users").document(userId).update(updates).await()
        true
    } catch (e: Exception) {
        false
    }
}

// Function to update password
suspend fun updateUserPassword(userId: String, newPassword: String): Boolean {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val hashedPassword = hashPassword(newPassword)
        val updates = hashMapOf<String, Any>(
            "password" to hashedPassword,
            "updatedAt" to Date()
        )
        firestore.collection("users").document(userId).update(updates).await()
        true
    } catch (e: Exception) {
        false
    }
}

// Function to check if email exists with same role (not allowed)
// Returns true if email exists with same role (different user) - meaning NOT allowed
// Also checks if same email + different role has same password (not allowed)
suspend fun checkEmailExistsWithSameRole(
    email: String,
    currentUserId: String,
    newPasswordHash: String? = null
): Boolean {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val snapshot = firestore.collection("users").whereEqualTo("email", email).get().await()

        if (snapshot.isEmpty) {
            return false // Email doesn't exist, can use it
        }

        // Get current user's role and password
        val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
        val currentUserRole = currentUserDoc.getString("role") ?: ""
        currentUserDoc.getString("password") ?: ""

        // Check all documents with this email
        for (doc in snapshot.documents) {
            if (doc.id == currentUserId) {
                continue // Skip current user
            }

            val existingRole = doc.getString("role") ?: ""
            val existingPassword = doc.getString("password") ?: ""

            // If email exists with same role (different user) - NOT allowed
            if (existingRole == currentUserRole) {
                return true
            }

            // If email exists with different role, check if password is the same (NOT allowed)
            if (existingRole != currentUserRole && newPasswordHash != null) {
                if (existingPassword == newPasswordHash) {
                    return true // Same email + different role + same password = NOT allowed
                }
            }
        }

        // If we get here, email exists but with different role and different password - allowed
        return false
    } catch (e: Exception) {
        false
    }
}

// Function to update email
suspend fun updateUserEmail(userId: String, newEmail: String): Boolean {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val updates = hashMapOf<String, Any>(
            "email" to newEmail,
            "updatedAt" to Date()
        )
        firestore.collection("users").document(userId).update(updates).await()
        true
    } catch (e: Exception) {
        false
    }
}

// Function to get userId from email (since app uses Firestore auth, not Firebase Auth)
suspend fun getUserIdFromEmail(email: String): String? {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val snapshot = firestore.collection("users").whereEqualTo("email", email).get().await()
        if (!snapshot.isEmpty) {
            snapshot.documents[0].id // Return document ID as userId
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

// Function to get userId from email and role (to ensure we get the correct user)
suspend fun getUserIdFromEmailAndRole(email: String, role: String): String? {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val snapshot = firestore.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("role", role)
            .get().await()
        if (!snapshot.isEmpty) {
            snapshot.documents[0].id // Return document ID as userId
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

// Function to get current user data
suspend fun getCurrentUserData(userId: String): Map<String, Any>? {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val doc = firestore.collection("users").document(userId).get().await()
        if (doc.exists()) {
            doc.data
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

// Function to update preferred translation language (for user role only)
suspend fun updatePreferredTranslationLanguage(userId: String, preference: String): Boolean {
    return try {
        val firestore = FirebaseFirestore.getInstance()
        val updates = hashMapOf<String, Any>(
            "preferredTranslationLanguage" to preference,
            "updatedAt" to Date()
        )
        firestore.collection("users").document(userId).update(updates).await()
        true
    } catch (e: Exception) {
        false
    }
}


@Composable
fun SettingsRow(label: String, showArrow: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color(0xFFF8F8F8))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                modifier = Modifier.weight(1f)
            )
            if (showArrow) {
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

@Composable
fun SettingsRowWithSubtitle(label: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color(0xFFF8F8F8))) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}

@Composable
fun AccountInfoScreen(navController: NavHostController, label: String) {
    val context = LocalContext.current

    // Load current email immediately from SharedPreferences (no delay)
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val email = remember { sharedPref.getString("user_email", "") ?: "" }
    val userRole = remember { sharedPref.getString("user_role", "user") ?: "user" }
    val notificationCount = rememberNotificationCount(email, userRole)

    Scaffold(
        topBar = {
            TopNav(notificationCount = notificationCount, onProfileClick = {
                { /* already in user settings */ }
            }, onTranslateClick = {
                context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
            }, onNotificationClick = {
                context.startActivity(Intent(context, NotificationActivity::class.java))
            })
        }, bottomBar = {
            BottomNav(onLearnClick = {
                context.startActivity(Intent(context, LearnActivity::class.java))
            }, onCameraClick = {
                context.startActivity(Intent(context, TranslationActivity::class.java))
            }, onQuizClick = {
                context.startActivity(Intent(context, QuizActivity::class.java))
            })
        }, containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = label,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(16.dp)
            )

            SettingsRowWithSubtitle(
                label = "Email", subtitle = email, onClick = {
                    navController.navigate("change_email/Change Email")
                })

            SettingsRow(
                label = "Change Password", showArrow = false, onClick = {
                    navController.navigate("change_password/Change Password")
                })
        }
    }
}

@Composable
fun UserProfileScreen(navController: NavHostController, label: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Load from SharedPreferences immediately (no delay)
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userEmail = remember { sharedPref.getString("user_email", null) }
    val userRole = remember { sharedPref.getString("user_role", "user") ?: "user" }

    // Try to load cached data from SharedPreferences first (immediate display)
    // Use role-specific cache keys to prevent mixing admin and user data
    val cacheKeyPrefix = "cached_${userRole}_"
    val cachedFirstName = remember { sharedPref.getString("${cacheKeyPrefix}firstName", "") ?: "" }
    val cachedLastName = remember { sharedPref.getString("${cacheKeyPrefix}lastName", "") ?: "" }
    val cachedProfileUrl = remember { sharedPref.getString("${cacheKeyPrefix}profileUrl", "") ?: "" }

    // Initialize with cached values immediately (no delay)
    var firstName by remember { mutableStateOf(cachedFirstName) }
    var lastName by remember { mutableStateOf(cachedLastName) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var profileUrl by remember { mutableStateOf(cachedProfileUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<String?>(null) }

    // Track original values to detect changes
    var originalFirstName by remember { mutableStateOf(cachedFirstName) }
    var originalLastName by remember { mutableStateOf(cachedLastName) }
    var originalProfileUrl by remember { mutableStateOf(cachedProfileUrl) }

    // Load user data from Firestore in background (update cache)
    LaunchedEffect(userEmail, userRole) {
        if (userEmail != null) {
            // Get userId from Firestore using email AND role to ensure we get the correct user
            val fetchedUserId = getUserIdFromEmailAndRole(userEmail, userRole)
            userId = fetchedUserId

            if (fetchedUserId != null) {
                val userData = getCurrentUserData(fetchedUserId)
                userData?.let {
                    val loadedFirstName = it["firstName"] as? String ?: ""
                    val loadedLastName = it["lastName"] as? String ?: ""
                    val loadedProfileUrl = it["profile"] as? String ?: ""

                    // Update state (will trigger recomposition)
                    firstName = loadedFirstName
                    lastName = loadedLastName
                    profileUrl = loadedProfileUrl

                    // Store original values
                    originalFirstName = loadedFirstName
                    originalLastName = loadedLastName
                    originalProfileUrl = loadedProfileUrl

                    // Cache in SharedPreferences for immediate loading next time
                    // Use role-specific cache keys to prevent mixing admin and user data
                    val cacheKeyPrefix = "cached_${userRole}_"
                    with(sharedPref.edit()) {
                        putString("${cacheKeyPrefix}firstName", loadedFirstName)
                        putString("${cacheKeyPrefix}lastName", loadedLastName)
                        putString("${cacheKeyPrefix}profileUrl", loadedProfileUrl)
                        apply()
                    }

                    // Only set imageUri if it's a local URI (not a Firebase Storage URL)
                    // Firebase Storage URLs should be displayed directly from profileUrl
                    if (profileUrl.isNotEmpty() && !profileUrl.startsWith("http://") && !profileUrl.startsWith(
                            "https://"
                        )
                    ) {
                        try {
                            val parsedUri = Uri.parse(profileUrl)
                            // Only set if it's a local URI (content:// or file://)
                            if (parsedUri.scheme == "content" || parsedUri.scheme == "file") {
                                imageUri = parsedUri
                            }
                        } catch (e: Exception) {
                            // Invalid URI, ignore
                        }
                    }
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Don't update profileUrl here - we'll upload and get the URL when saving
            // This way we can detect if a new image was selected
        }
    }

    val sharedPrefForNotification = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userEmailForNotification = remember { sharedPrefForNotification.getString("user_email", "") ?: "" }
    val userRoleForNotification = remember { sharedPrefForNotification.getString("user_role", "user") ?: "user" }
    val notificationCountForPersonalInfo = rememberNotificationCount(userEmailForNotification, userRoleForNotification)

    Scaffold(
        topBar = {
            TopNav(
                notificationCount = notificationCountForPersonalInfo,
                onProfileClick = { /* already in user settings */ },
                onTranslateClick = {
                    context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
                },
                onNotificationClick = {
                    context.startActivity(Intent(context, NotificationActivity::class.java))
                })
        }, bottomBar = {
            BottomNav(onLearnClick = {
                context.startActivity(Intent(context, LearnActivity::class.java))
            }, onCameraClick = {
                context.startActivity(Intent(context, TranslationActivity::class.java))
            }, onQuizClick = {
                context.startActivity(Intent(context, QuizActivity::class.java))
            })
        }, containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .width(300.dp)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            // Priority 1: Show newly selected image (local URI from image picker)
                            imageUri != null && !imageUri.toString()
                                .startsWith("http://") && !imageUri.toString()
                                .startsWith("https://") -> {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Profile Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            // Priority 2: Show saved profile picture from base64 data URI (persists after logout/login)
                            profileUrl.isNotEmpty() && profileUrl.startsWith("data:image") -> {
                                // Decode base64 data URI to Bitmap for display
                                val bitmap = remember(profileUrl) {
                                    try {
                                        // Extract base64 string from data URI: "data:image/jpeg;base64,<base64_string>"
                                        val base64String = profileUrl.substringAfter(",")
                                        if (base64String.isEmpty()) {
                                            return@remember null
                                        }
                                        val imageBytes = Base64.decode(base64String, Base64.NO_WRAP)
                                        val decodedBitmap = BitmapFactory.decodeByteArray(
                                            imageBytes,
                                            0,
                                            imageBytes.size
                                        )
                                        decodedBitmap
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Profile Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    // Fallback to placeholder if decoding fails
                                    AestheticProfilePlaceholder(
                                        firstName = firstName, drawableRes = R.drawable.profile
                                    )
                                }
                            }
                            // Priority 3: Show saved profile picture from URL (http/https)
                            profileUrl.isNotEmpty() && (profileUrl.startsWith("http://") || profileUrl.startsWith(
                                "https://"
                            )) -> {
                                AsyncImage(
                                    model = profileUrl,
                                    contentDescription = "Profile Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    error = painterResource(id = R.drawable.profile)
                                )
                            }

                            profileUrl.isNotEmpty() -> {
                                // Try to load from local URI if it's a file path
                                // Note: Local URIs may not persist after app restart
                                val parsedUri = runCatching { Uri.parse(profileUrl) }.getOrNull()
                                if (parsedUri != null && (parsedUri.scheme == "content" || parsedUri.scheme == "file")) {
                                    AsyncImage(
                                        model = parsedUri,
                                        contentDescription = "Profile Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        error = painterResource(id = R.drawable.profile)
                                    )
                                } else {
                                    // Invalid URI or file doesn't exist, show placeholder
                                    AestheticProfilePlaceholder(
                                        firstName = firstName, drawableRes = R.drawable.profile
                                    )
                                }
                            }
                            else -> {
                                AestheticProfilePlaceholder(
                                    firstName = firstName, drawableRes = R.drawable.profile
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .offset(x = -6.dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFACC15))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Upload Photo",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${firstName.ifEmpty { "Your" }} ${lastName.ifEmpty { "Name" }}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    placeholder = {
                        Text(
                            "Firstname",
                            color = Color.Black,
                            fontFamily = InterFontFamily,
                            fontSize = 16.sp
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = {
                        Text(
                            "Lastname",
                            color = Color.Black,
                            fontFamily = InterFontFamily,
                            fontSize = 16.sp
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Check if there are any changes
                val trimmedFirstName = firstName.trim()
                val trimmedLastName = lastName.trim()
                val firstNameChanged = trimmedFirstName != originalFirstName
                val lastNameChanged = trimmedLastName != originalLastName
                // Profile changed if a new image was selected (imageUri is different from saved profileUrl)
                val profileChanged = imageUri != null && (imageUri.toString() != originalProfileUrl)
                val hasChanges = firstNameChanged || lastNameChanged || profileChanged

                Button(
                    onClick = {
                        if (isLoading) return@Button // Prevent multiple clicks

                        if (userId == null) {
                            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // If only profile is being changed, require both firstName and lastName to have values
                        if (profileChanged && !firstNameChanged && !lastNameChanged) {
                            if (trimmedFirstName.isEmpty() || trimmedLastName.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "First name and last name must be filled before changing profile picture",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }
                        }

                        // If firstName or lastName is being changed, both must have values
                        if (firstNameChanged || lastNameChanged) {
                            if (trimmedFirstName.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "First name is required",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            if (trimmedLastName.isEmpty()) {
                                Toast.makeText(context, "Last name is required", Toast.LENGTH_SHORT)
                                    .show()
                                return@Button
                            }
                        }

                        isLoading = true
                        coroutineScope.launch {
                            var finalProfileUrl = profileUrl

                            // If a new image was selected (imageUri is not null and different from saved profileUrl)
                            // and it's a local URI (not already a URL), upload it to Firebase Storage
                            val currentImageUri = imageUri
                            if (currentImageUri != null && profileChanged) {
                                val uriString = currentImageUri.toString()
                                // Check if it's a local URI (not already a URL or data URI)
                                if (!uriString.startsWith("http://") && !uriString.startsWith("https://") && !uriString.startsWith(
                                        "data:image"
                                    )
                                ) {
                                    // Convert image to base64 (FREE - no Firebase Storage/billing needed)
                                    try {
                                        val (base64Uri, errorMessage) = convertImageToBase64(
                                            context,
                                            currentImageUri
                                        )
                                        if (base64Uri != null) {
                                            finalProfileUrl =
                                                base64Uri // Store base64 data URI in Firestore
                                        } else {
                                            isLoading = false
                                            val errorMsg = errorMessage ?: "Unknown error occurred"
                                            Toast.makeText(
                                                context,
                                                "Failed to process image: $errorMsg",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Failed to process image: ${e.localizedMessage ?: e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }
                                } else {
                                    // Already a URL or data URI, use it as is
                                    finalProfileUrl = uriString
                                }
                            }

                            val success = updateUserProfile(
                                userId = userId!!,
                                firstName = trimmedFirstName,
                                lastName = trimmedLastName,
                                profile = finalProfileUrl
                            )
                            isLoading = false
                            if (success) {
                                // Update cache in SharedPreferences for immediate loading next time
                                // Use role-specific cache keys to prevent mixing admin and user data
                                val cacheKeyPrefix = "cached_${userRole}_"
                                with(sharedPref.edit()) {
                                    putString("${cacheKeyPrefix}firstName", trimmedFirstName)
                                    putString("${cacheKeyPrefix}lastName", trimmedLastName)
                                    putString("${cacheKeyPrefix}profileUrl", finalProfileUrl)
                                    apply()
                                }

                                Toast.makeText(
                                    context,
                                    "Profile updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Navigate back to settings
                                navController.popBackStack()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to update profile",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = hasChanges,
                    modifier = Modifier
                        .width(150.dp)
                        .height(35.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFACC15),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFFEEEEEE),
                        disabledContentColor = Color.Black
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Save Changes",
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AestheticProfilePlaceholder(firstName: String, drawableRes: Int) {
    val initial = firstName.firstOrNull()?.uppercaseChar()?.toString() ?: ""

    if (firstName.isEmpty()) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "Default Profile",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFFF5F5F5), 1.0f to Color(0xFFBEBEBE)
                        )
                    ),

                    shape = CircleShape
                ), contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
fun ChangeEmailScreen(navController: NavHostController, label: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textColor = Color.Black

    // Load current email immediately from SharedPreferences
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val initialEmail = remember { sharedPref.getString("user_email", "") ?: "" }
    val userRole = remember { sharedPref.getString("user_role", "user") ?: "user" }

    var newEmail by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<String?>(null) }
    var originalEmail by remember { mutableStateOf(initialEmail) }

    // Load userId and verify email in background
    LaunchedEffect(Unit) {
        if (initialEmail.isNotEmpty()) {
            // Get userId from Firestore using email AND role to ensure we get the correct user
            val fetchedUserId = getUserIdFromEmailAndRole(initialEmail, userRole)
            userId = fetchedUserId

            if (fetchedUserId != null) {
                val userData = getCurrentUserData(fetchedUserId)
                userData?.let {
                    val currentEmail = it["email"] as? String ?: ""
                    if (currentEmail.isNotEmpty() && currentEmail != newEmail) {
                        newEmail = currentEmail
                        originalEmail = currentEmail
                    }
                }
            }
        }
    }

    val sharedPrefForNotification = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userEmailForNotification = remember { sharedPrefForNotification.getString("user_email", "") ?: "" }
    val userRoleForNotification = remember { sharedPrefForNotification.getString("user_role", "user") ?: "user" }
    val notificationCountForScreen = rememberNotificationCount(userEmailForNotification, userRoleForNotification)

    Scaffold(
        topBar = {
            TopNav(notificationCount = notificationCountForScreen, onProfileClick = {
                { /* already in user settings */ }
            }, onTranslateClick = {
                context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
            }, onNotificationClick = {
                context.startActivity(Intent(context, NotificationActivity::class.java))
            })
        }, bottomBar = {
            BottomNav(
                onLearnClick = {
                    context.startActivity(
                        Intent(
                            context, LearnActivity::class.java
                        )
                    )
                },
                onCameraClick = {
                    context.startActivity(
                        Intent(
                            context, TranslationActivity::class.java
                        )
                    )
                },
                onQuizClick = { context.startActivity(Intent(context, QuizActivity::class.java)) })
        }, containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(300.dp)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    placeholder = {
                        Text(
                            "New email address",
                            color = Color.Black,
                            fontFamily = InterFontFamily,
                            fontSize = 16.sp
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.Black,
                        cursorColor = Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = {
                        Text(
                            "Password",
                            color = textColor,
                            fontFamily = InterFontFamily,
                            fontSize = 16.sp
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = textColor
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        val image =
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = "Toggle Password",
                                tint = textColor
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedIndicatorColor = textColor,
                        unfocusedIndicatorColor = textColor,
                        cursorColor = textColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Check if email changed
                val emailChanged = newEmail.trim() != originalEmail
                val hasChanges = emailChanged

                Button(
                    onClick = {
                        if (isLoading) return@Button // Prevent multiple clicks

                        if (userId == null) {
                            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Validation
                        val trimmedEmail = newEmail.trim()
                        if (trimmedEmail.isEmpty()) {
                            Toast.makeText(context, "Please enter email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Validate email format
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                            Toast.makeText(
                                context,
                                "Please enter a valid email address",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        // Additional email validation - check for proper email structure
                        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                            Toast.makeText(
                                context,
                                "Please enter a valid email address",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        val emailParts = trimmedEmail.split("@")
                        if (emailParts.size != 2 || emailParts[0].isEmpty() || emailParts[1].isEmpty()) {
                            Toast.makeText(
                                context,
                                "Please enter a valid email address",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        val domainParts = emailParts[1].split(".")
                        if (domainParts.size < 2 || domainParts.any { it.isEmpty() }) {
                            Toast.makeText(
                                context,
                                "Please enter a valid email address",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        // If email changed, password is required
                        if (emailChanged && password.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Please enter current password to change email",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            // If email changed, verify password first
                            if (emailChanged) {
                                val userData = getCurrentUserData(userId!!)
                                val storedPasswordHash = userData?.get("password") as? String ?: ""
                                val passwordHash = hashPassword(password)

                                if (storedPasswordHash != passwordHash) {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Password is incorrect",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }

                                // Get current user's password hash to check
                                val currentPasswordHash = passwordHash

                                // Check if email exists with same role OR same email + different role + same password (not allowed)
                                val emailExistsWithSameRole = checkEmailExistsWithSameRole(
                                    trimmedEmail,
                                    userId!!,
                                    currentPasswordHash
                                )
                                if (emailExistsWithSameRole) {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Email already exists with the same role, or same email with different role has the same password",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }
                            }

                            // Update email in Firestore
                            val success = updateUserEmail(userId!!, trimmedEmail)

                            if (success) {
                                // Update SharedPreferences with new email
                                val sharedPref = context.getSharedPreferences(
                                    "user_session",
                                    Context.MODE_PRIVATE
                                )
                                with(sharedPref.edit()) {
                                    putString("user_email", trimmedEmail)
                                    apply()
                                }

                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Email updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Navigate back to settings
                                navController.popBackStack()
                            } else {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Failed to update email",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = hasChanges,
                    modifier = Modifier
                        .width(150.dp)
                        .height(35.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFACC15),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFFEEEEEE),
                        disabledContentColor = Color.Black
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Save Changes",
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(navController: NavHostController, label: String) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<String?>(null) }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textColor = Color.Black

    // Get userId when screen opens
    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", null)
        val userRole = sharedPref.getString("user_role", "user") ?: "user"

        if (userEmail != null) {
            // Get userId from Firestore using email AND role to ensure we get the correct user
            val fetchedUserId = getUserIdFromEmailAndRole(userEmail, userRole)
            userId = fetchedUserId
        }
    }

    val sharedPrefForNotification = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userEmailForNotification = remember { sharedPrefForNotification.getString("user_email", "") ?: "" }
    val userRoleForNotification = remember { sharedPrefForNotification.getString("user_role", "user") ?: "user" }
    val notificationCountForScreen = rememberNotificationCount(userEmailForNotification, userRoleForNotification)

    Scaffold(
        topBar = {
            TopNav(notificationCount = notificationCountForScreen, onProfileClick = {
                { /* already in user settings */ }
            }, onTranslateClick = {
                context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
            }, onNotificationClick = {
                context.startActivity(Intent(context, NotificationActivity::class.java))
            })
        }, bottomBar = {
            BottomNav(
                onLearnClick = {
                    context.startActivity(
                        Intent(
                            context, LearnActivity::class.java
                        )
                    )
                },
                onCameraClick = {
                    context.startActivity(
                        Intent(
                            context, TranslationActivity::class.java
                        )
                    )
                },
                onQuizClick = { context.startActivity(Intent(context, QuizActivity::class.java)) })
        }, containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(300.dp)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    placeholder = {
                        Text(
                            "Current password",
                            color = textColor,
                            fontFamily = InterFontFamily,
                            fontSize = 16.sp
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = textColor
                    ),
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        val image =
                            if (currentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = "Toggle Password",
                                tint = textColor
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedIndicatorColor = textColor,
                        unfocusedIndicatorColor = textColor,
                        cursorColor = textColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = {
                        Text(
                            "New password",
                            color = textColor,
                            fontFamily = InterFontFamily,
                            fontSize = 16.sp
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = textColor
                    ),
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        val image =
                            if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = "Toggle Password",
                                tint = textColor
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedIndicatorColor = textColor,
                        unfocusedIndicatorColor = textColor,
                        cursorColor = textColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = {
                        Text(
                            "Confirm password",
                            color = textColor,
                            fontFamily = InterFontFamily,
                            fontSize = 16.sp
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = textColor
                    ),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        val image =
                            if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = "Toggle Password",
                                tint = textColor
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedIndicatorColor = textColor,
                        unfocusedIndicatorColor = textColor,
                        cursorColor = textColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Check if there are any changes
                val hasChanges =
                    currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()

                Button(
                    onClick = {
                        if (isLoading) return@Button // Prevent multiple clicks

                        if (userId == null) {
                            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Validation
                        if (currentPassword.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Please enter current password",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        if (newPassword.isEmpty()) {
                            Toast.makeText(context, "Please enter new password", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }
                        // Password requirements (same as sign up)
                        if (newPassword.length < 8) {
                            Toast.makeText(
                                context,
                                "Password must be at least 8 characters long",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        if (!newPassword.any { it.isDigit() }) {
                            Toast.makeText(
                                context,
                                "Password must contain at least one number",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        if (!newPassword.any { it.isLetter() }) {
                            Toast.makeText(
                                context,
                                "Password must contain at least one letter",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        if (!newPassword.any { !it.isLetterOrDigit() }) {
                            Toast.makeText(
                                context,
                                "Password must include at least one special character (e.g., !@#\$%^&*)",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            // Verify current password
                            val userData = getCurrentUserData(userId!!)
                            val storedPasswordHash = userData?.get("password") as? String ?: ""
                            val currentPasswordHash = hashPassword(currentPassword)

                            if (storedPasswordHash != currentPasswordHash) {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Current password is incorrect",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            // Get current user's email to check if same email + different role has same password
                            val currentEmail = userData?.get("email") as? String ?: ""
                            val newPasswordHash = hashPassword(newPassword)

                            // Check if same email + different role has same password (not allowed)
                            if (currentEmail.isNotEmpty()) {
                                val emailExistsWithSamePassword = checkEmailExistsWithSameRole(
                                    currentEmail,
                                    userId!!,
                                    newPasswordHash
                                )
                                if (emailExistsWithSamePassword) {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Cannot use same password. Same email with different role must have different password",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }
                            }

                            // Update password
                            val success = updateUserPassword(userId!!, newPassword)
                            isLoading = false

                            if (success) {
                                Toast.makeText(
                                    context,
                                    "Password updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Navigate back to settings
                                navController.popBackStack()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to update password",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = hasChanges,
                    modifier = Modifier
                        .width(150.dp)
                        .height(35.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFACC15),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFFEEEEEE),
                        disabledContentColor = Color.Black
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Save Changes",
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(label: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPref = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userEmail = remember { sharedPref.getString("user_email", "") ?: "" }
    val userRole = remember { sharedPref.getString("user_role", "user") ?: "user" }
    
    val options = listOf("ENG", "FIL")
    
    // Load current preference from SharedPreferences, default to "ENG"
    val savedPreference = remember { 
        sharedPref.getString("preferred_translation_language", "ENG") ?: "ENG"
    }
    var selectedOption by remember { mutableStateOf(savedPreference) }
    var isLoading by remember { mutableStateOf(false) }

    // Only allow this feature for user role
    val isUserRole = userRole == "user"

    // Load preference from Firestore on init (if user role)
    LaunchedEffect(userEmail, userRole) {
        if (isUserRole && userEmail.isNotEmpty()) {
            scope.launch {
                try {
                    val userId = getUserIdFromEmailAndRole(userEmail, userRole)
                    if (userId != null) {
                        val userData = getCurrentUserData(userId)
                        val dbPreference = userData?.get("preferredTranslationLanguage") as? String
                        if (dbPreference != null && (dbPreference == "ENG" || dbPreference == "FIL")) {
                            selectedOption = dbPreference
                            // Update SharedPreferences to match
                            with(sharedPref.edit()) {
                                putString("preferred_translation_language", dbPreference)
                                apply()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // If error, use SharedPreferences value
                }
            }
        }
    }

    val sharedPrefForNotification = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val userEmailForNotification = remember { sharedPrefForNotification.getString("user_email", "") ?: "" }
    val userRoleForNotification = remember { sharedPrefForNotification.getString("user_role", "user") ?: "user" }
    val notificationCountForScreen = rememberNotificationCount(userEmailForNotification, userRoleForNotification)

    Scaffold(
        topBar = {
            TopNav(notificationCount = notificationCountForScreen, onProfileClick = {
                { /* already in user settings */ }
            }, onTranslateClick = {
                context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
            }, onNotificationClick = {
                context.startActivity(Intent(context, NotificationActivity::class.java))
            })
        }, bottomBar = {
            BottomNav(
                onLearnClick = {
                    context.startActivity(
                        Intent(
                            context, LearnActivity::class.java
                        )
                    )
                },
                onCameraClick = {
                    context.startActivity(
                        Intent(
                            context, TranslationActivity::class.java
                        )
                    )
                },
                onQuizClick = { context.startActivity(Intent(context, QuizActivity::class.java)) })
        }, containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = label,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(16.dp)
            )

            PreferenceDropdownBox(
                label = "Preferred Sign to Text Translation",
                options = options,
                selectedOption = selectedOption,
                onOptionSelected = { newOption ->
                    if (!isUserRole) {
                        Toast.makeText(
                            context,
                            "This feature is only available for users",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@PreferenceDropdownBox
                    }
                    
                    if (isLoading) return@PreferenceDropdownBox
                    
                    selectedOption = newOption
                    
                    // Save to SharedPreferences immediately
                    with(sharedPref.edit()) {
                        putString("preferred_translation_language", newOption)
                        apply()
                    }
                    
                    // Save to Firestore
                    scope.launch {
                        isLoading = true
                        try {
                            val userId = getUserIdFromEmailAndRole(userEmail, userRole)
                            if (userId != null) {
                                val success = updatePreferredTranslationLanguage(userId, newOption)
                                isLoading = false
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "Preference saved successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to save preference",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "User not found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(
                                context,
                                "Error saving preference: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
        }
    }
}

@Composable
fun PreferenceDropdownBox(
    label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                modifier = Modifier.weight(1f)
            )

            Box {
                val buttonWidth = 100.dp
                Button(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFACC15), contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedOption, fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    modifier = Modifier
                        .width(buttonWidth)
                        .background(Color(0xFFFACC15))
                ) {
                    options.filter { it != selectedOption }.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            }, text = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }, modifier = Modifier.background(Color(0xFFFACC15))
                        )
                    }
                }
            }
        }

        Divider(
            color = Color.LightGray, thickness = 1.dp, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAccountInfoScreen() {
    val navController = rememberNavController()
    AccountInfoScreen(navController, "Account Information")
}

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    val navController = rememberNavController()
    UserProfileScreen(navController, "Personal Information")
}