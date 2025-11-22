package com.example.expressora.dashboard.user.notification

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.expressora.utils.RoleValidationUtil
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressora.R
import com.example.expressora.components.top_nav.TopNav
import com.example.expressora.components.user_bottom_nav.BottomNav
import com.example.expressora.dashboard.user.community_space.CommunitySpaceActivity
import com.example.expressora.dashboard.user.learn.LearnActivity
import com.example.expressora.dashboard.user.quiz.QuizActivity
import com.example.expressora.dashboard.user.settings.SettingsActivity
import com.example.expressora.dashboard.user.translation.TranslationActivity
import com.example.expressora.ui.theme.InterFontFamily
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.tasks.await
import com.example.expressora.backend.NotificationRepository
import com.example.expressora.models.Notification as FirebaseNotification
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Validate role before showing screen - redirect to login if not user role
        RoleValidationUtil.validateRoleAndRedirect(this, "user") { isValid ->
            if (!isValid) {
                return@validateRoleAndRedirect // Will redirect to login
            }
            
            // Show screen only if role is valid
            setContent {
                NotificationScreen()
            }
        }
    }
}

data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val time: String,
    val iconRes: Int? = null,
    val isRead: Boolean = false
)

// Helper function to get userId from email and role
suspend fun getUserIdFromEmail(email: String, role: String): String? {
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

// Helper function to format time
fun formatTimeAgo(date: java.util.Date?): String {
    if (date == null) return "Just now"
    val now = System.currentTimeMillis()
    val time = date.time
    val diff = now - time
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
            "$minutes min${if (minutes > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff).toInt()
            "$hours hr${if (hours > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
            "$days day${if (days > 1) "s" else ""} ago"
        }
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(date)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationRepository = remember { NotificationRepository() }
    val sharedPref = remember { context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE) }
    val userEmail = remember { sharedPref.getString("user_email", "") ?: "" }
    val userRole = remember { sharedPref.getString("user_role", "user") ?: "user" }

    val bgColor = Color(0xFFF8F8F8)
    val cardColorUnread = Color(0xFFFFF4C2)
    val cardColorRead = Color.White
    val textColor = Color.Black
    val subtitleColor = Color(0xFF666666)

    val notifications = remember { mutableStateListOf<NotificationItem>() }
    var isLoading by remember { mutableStateOf(true) }
    // Map to track Firebase notification IDs (String) for each NotificationItem (Int id)
    val firebaseIdMap = remember { mutableStateMapOf<Int, String>() }

    val visibilityStates = remember {
        mutableStateMapOf<Int, MutableTransitionState<Boolean>>()
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val listState = rememberLazyListState()
    var isClearingAll by remember { mutableStateOf(false) }
    var lastClearTime by remember { mutableStateOf(0L) }

    // Load notifications from Firebase
    fun loadNotifications() {
        scope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val userId = getUserIdFromEmail(userEmail, userRole)
                    if (userId != null) {
                        android.util.Log.d("NotificationScreen", "Loading notifications for userId: $userId")
                        val result = notificationRepository.getUserNotifications(userId)
                        result.onSuccess { firebaseNotifications ->
                            android.util.Log.d("NotificationScreen", "Loaded ${firebaseNotifications.size} notifications")
                            withContext(Dispatchers.Main) {
                                notifications.clear()
                                visibilityStates.clear()
                                firebaseIdMap.clear()
                                firebaseNotifications.forEach { firebaseNotif ->
                                    val itemId = firebaseNotif.id.hashCode() // Convert String ID to Int for compatibility
                                    val notificationItem = NotificationItem(
                                        id = itemId,
                                        title = firebaseNotif.title,
                                        message = firebaseNotif.message,
                                        time = formatTimeAgo(firebaseNotif.createdAt),
                                        iconRes = null,
                                        isRead = firebaseNotif.isRead
                                    )
                                    notifications.add(notificationItem)
                                    firebaseIdMap[itemId] = firebaseNotif.id // Map Int id to Firebase String id
                                    visibilityStates[itemId] = MutableTransitionState(true)
                                }
                                isLoading = false
                            }
                        }.onFailure { e ->
                            android.util.Log.e("NotificationScreen", "Failed to load notifications: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    } else {
                        android.util.Log.w("NotificationScreen", "userId is null for email: $userEmail, role: $userRole")
                        withContext(Dispatchers.Main) {
                            isLoading = false
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationScreen", "Exception loading notifications: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        }
    }

    // Real-time notification listener - updates instantly without reloading
    LaunchedEffect(userEmail, userRole) {
        val userId = withContext(Dispatchers.IO) {
            getUserIdFromEmail(userEmail, userRole)
        }
        
        if (userId != null) {
            isLoading = true
            // Start real-time listener for instant updates
            notificationRepository.getUserNotificationsRealtime(userId)
                .collectLatest { firebaseNotifications ->
                    // Update UI instantly on Main thread
                    withContext(Dispatchers.Main) {
                        // COMPLETELY IGNORE updates while clearing all to prevent double-trigger
                        if (isClearingAll) {
                            return@withContext // Exit immediately, don't process anything
                        }
                        
                        // Ignore updates for 2 seconds after clearing to prevent race conditions
                        val timeSinceClear = System.currentTimeMillis() - lastClearTime
                        if (timeSinceClear < 2000 && notifications.isEmpty() && firebaseNotifications.isNotEmpty()) {
                            return@withContext // Ignore re-adds right after clearing
                        }
                        
                        // If Firebase is empty and local is not, clear local (normal deletion)
                        if (notifications.isNotEmpty() && firebaseNotifications.isEmpty() && !isClearingAll) {
                            notifications.clear()
                            visibilityStates.clear()
                            firebaseIdMap.clear()
                            return@withContext
                        }
                        
                        // Only update if there are actual changes to avoid unnecessary recompositions
                        val currentIds = notifications.mapNotNull { firebaseIdMap[it.id] }.toSet()
                        val newIds = firebaseNotifications.map { it.id }.toSet()
                        
                        // If lists are different, update
                        if (currentIds != newIds || notifications.size != firebaseNotifications.size) {
                            // Remove items that no longer exist
                            val itemsToRemove = notifications.filter { 
                                val firebaseId = firebaseIdMap[it.id]
                                firebaseId != null && !firebaseNotifications.any { it.id == firebaseId }
                            }
                            itemsToRemove.forEach { notif ->
                                notifications.remove(notif)
                                visibilityStates.remove(notif.id)
                                firebaseIdMap.remove(notif.id)
                            }
                            
                            // Add or update existing items
                            firebaseNotifications.forEach { firebaseNotif ->
                                val itemId = firebaseNotif.id.hashCode()
                                val existingIndex = notifications.indexOfFirst { it.id == itemId }
                                
                                if (existingIndex == -1) {
                                    // New notification - add with animation
                                    val notificationItem = NotificationItem(
                                        id = itemId,
                                        title = firebaseNotif.title,
                                        message = firebaseNotif.message,
                                        time = formatTimeAgo(firebaseNotif.createdAt),
                                        iconRes = null,
                                        isRead = firebaseNotif.isRead
                                    )
                                    notifications.add(0, notificationItem) // Add to top
                                    firebaseIdMap[itemId] = firebaseNotif.id
                                    visibilityStates[itemId] = MutableTransitionState(true)
                                } else {
                                    // Update existing notification (e.g., read status)
                                    val existing = notifications[existingIndex]
                                    if (existing.isRead != firebaseNotif.isRead) {
                                        notifications[existingIndex] = existing.copy(isRead = firebaseNotif.isRead)
                                    }
                                }
                            }
                        }
                        isLoading = false
                    }
                }
        } else {
            isLoading = false
        }
    }

    fun fetchNewNotifications() {
        // Real-time listener handles updates automatically
        // This is just for manual refresh if needed
        scope.launch {
            isRefreshing = true
        }
        // Real-time listener will update automatically, just reset refresh state
        scope.launch {
            delay(500) // Brief delay for visual feedback
            isRefreshing = false
        }
    }

    Scaffold(topBar = {
        TopNav(notificationCount = notifications.count { !it.isRead }, onProfileClick = {
            context.startActivity(
                Intent(
                    context, SettingsActivity::class.java
                )
            )
        }, onTranslateClick = {
            context.startActivity(
                Intent(
                    context, CommunitySpaceActivity::class.java
                )
            )
        }, onNotificationClick = {
            { /* already in notification */ }
        })
    }, bottomBar = {
        BottomNav(onLearnClick = {
            context.startActivity(
                Intent(
                    context, LearnActivity::class.java
                )
            )
        }, onCameraClick = {
            context.startActivity(
                Intent(
                    context, TranslationActivity::class.java
                )
            )
        }, onQuizClick = { context.startActivity(Intent(context, QuizActivity::class.java)) })
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(paddingValues)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    color = textColor
                )
                if (notifications.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                scope.launch(Dispatchers.Main) {
                                    // Prevent double-trigger - check and set flag immediately
                                    if (isClearingAll || notifications.isEmpty()) return@launch
                                    
                                    // Set flag FIRST to block real-time listener
                                    isClearingAll = true
                                    
                                    // Collect all IDs and Firebase IDs BEFORE clearing
                                    val allIds = notifications.map { it.id }.toList()
                                    val allFirebaseIds = allIds.mapNotNull { firebaseIdMap[it] }
                                    
                                    // Start all animations instantly with smooth cascade effect
                                    allIds.forEachIndexed { index, id ->
                                        visibilityStates[id]?.targetState = false
                                        // Smooth stagger for beautiful cascade (15ms)
                                        if (index < allIds.size - 1) {
                                            delay(15)
                                        }
                                    }
                                    
                                    // Wait for animations to complete (120ms animation + 30ms buffer)
                                    delay(150)
                                    
                                    // Clear all immediately - this ensures UI updates right away
                                    notifications.clear()
                                    visibilityStates.clear()
                                    firebaseIdMap.clear()
                                    
                                    // Record clear time to prevent re-adds
                                    lastClearTime = System.currentTimeMillis()
                                    
                                    // Delete from Firebase in background (non-blocking)
                                    launch(Dispatchers.IO) {
                                        allFirebaseIds.forEach { firebaseId ->
                                            try {
                                                notificationRepository.deleteNotification(firebaseId)
                                            } catch (e: Exception) {
                                                // Ignore errors - don't block UI
                                            }
                                        }
                                        // Keep flag set longer to prevent real-time listener from interfering
                                        // Wait for Firebase to process all deletions
                                        delay(2000) // Longer delay to ensure Firebase processes everything
                                        withContext(Dispatchers.Main) {
                                            isClearingAll = false
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(
                            text = "Clear All",
                            fontSize = 14.sp,
                            color = subtitleColor,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            Divider(color = Color(0xFFB8BCC2), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { fetchNewNotifications() },
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        backgroundColor = bgColor,
                        contentColor = Color.Black,
                        scale = true
                    )
                }) {
                if (notifications.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "No notifications yet.",
                            color = subtitleColor,
                            fontSize = 16.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 8.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = notifications.toList(),
                            key = { _, item -> item.id }) { _, notif ->
                            val visibleState = visibilityStates.getOrPut(notif.id) {
                                MutableTransitionState(true)
                            }

                            val swipeState = rememberSwipeToDismissBoxState(
                                initialValue = SwipeToDismissBoxValue.Settled,
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        // Start exit animation immediately - instant response
                                        visibleState.targetState = false
                                        // Delete from Firebase in background (non-blocking, doesn't affect UI)
                                        val firebaseId = firebaseIdMap[notif.id]
                                        if (firebaseId != null) {
                                            scope.launch(Dispatchers.IO) {
                                                notificationRepository.deleteNotification(firebaseId)
                                            }
                                        }
                                        true
                                    } else false
                                })

                            SwipeToDismissBox(
                                state = swipeState,
                                enableDismissFromEndToStart = true,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {},
                                modifier = Modifier.fillMaxWidth()) {
                                AnimatedVisibility(
                                    visibleState = visibleState,
                                    enter = fadeIn(tween(120, easing = FastOutSlowInEasing)) + expandVertically(tween(120, easing = FastOutSlowInEasing)),
                                    exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) + shrinkVertically(tween(120, easing = FastOutSlowInEasing))
                                ) {
                                    NotificationCard(
                                        item = notif,
                                        unreadBackground = cardColorUnread,
                                        readBackground = cardColorRead,
                                        textColor = textColor,
                                        subtitleColor = subtitleColor,
                                        onClick = {
                                            // Instant mark as read - no delay, immediate UI update
                                            val idx = notifications.indexOfFirst { it.id == notif.id }
                                            if (idx != -1 && !notifications[idx].isRead) {
                                                // Update state immediately for instant visual feedback
                                                notifications[idx] = notifications[idx].copy(isRead = true)
                                                // Mark as read in Firebase (background, non-blocking)
                                                val firebaseId = firebaseIdMap[notif.id]
                                                if (firebaseId != null) {
                                                    scope.launch(Dispatchers.IO) {
                                                        notificationRepository.markAsRead(firebaseId)
                                                    }
                                                }
                                            }
                                        })
                                }
                            }
                            
                            // Remove item after exit animation completes smoothly
                            if (!visibleState.currentState && !visibleState.targetState) {
                                LaunchedEffect(notif.id) {
                                    // Ultra-fast animation completion (120ms for snappier feel)
                                    delay(120)
                                    // Instant removal - no blocking operations
                                    if (notifications.contains(notif)) {
                                        notifications.remove(notif)
                                        visibilityStates.remove(notif.id)
                                        firebaseIdMap.remove(notif.id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    unreadBackground: Color,
    readBackground: Color,
    textColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (item.isRead) readBackground else unreadBackground,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "bgColorAnim"
    )
    val iconBackgroundColor by animateColorAsState(
        targetValue = if (item.isRead) readBackground else unreadBackground,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "iconColorAnim"
    )
    val icon = item.iconRes ?: R.drawable.expressora_logo
    val elevation = if (item.isRead) 2.dp else 6.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = "Notification Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.message,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    color = subtitleColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = item.time,
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                color = subtitleColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationScreenPreview() {
    NotificationScreen()
}