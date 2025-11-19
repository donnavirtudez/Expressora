package com.example.expressora.dashboard.user.notification

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import kotlinx.coroutines.tasks.await
import com.example.expressora.backend.NotificationRepository
import com.example.expressora.models.Notification as FirebaseNotification
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotificationScreen()
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

    // Load notifications on first launch
    LaunchedEffect(Unit) {
        loadNotifications()
    }
    
    // Refresh notifications when user returns to this screen
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            loadNotifications()
        }
    }
    
    // Periodic refresh every 5 seconds when screen is visible
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // Refresh every 5 seconds
            loadNotifications()
        }
    }
    
    // Trigger refresh when screen is disposed (user navigated away)
    DisposableEffect(Unit) {
        onDispose {
            // Increment refresh key so next time screen is shown, it refreshes
            refreshKey++
        }
    }

    fun fetchNewNotifications() {
        scope.launch {
            isRefreshing = true
            withContext(Dispatchers.IO) {
                val userId = getUserIdFromEmail(userEmail, userRole)
                if (userId != null) {
                    val result = notificationRepository.getUserNotifications(userId)
                    result.onSuccess { firebaseNotifications ->
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
                            isRefreshing = false
                            listState.scrollToItem(0)
                        }
                    }.onFailure {
                        withContext(Dispatchers.Main) {
                            isRefreshing = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isRefreshing = false
                    }
                }
            }
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
                                scope.launch {
                                    // Delete all from Firebase
                                    withContext(Dispatchers.IO) {
                                        firebaseIdMap.values.forEach { firebaseId ->
                                            notificationRepository.deleteNotification(firebaseId)
                                        }
                                    }
                                    notifications.forEach {
                                        visibilityStates[it.id]?.targetState = false
                                        delay(100)
                                    }
                                    delay(400)
                                    notifications.clear()
                                    visibilityStates.clear()
                                    firebaseIdMap.clear()
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
                        )
                    ) {
                        // Filter out items that are animating out or fully removed to prevent gaps
                        val visibleNotifications = notifications.filter { notif ->
                            val visibleState = visibilityStates[notif.id]
                            // Only show items that are currently visible and not animating out
                            visibleState?.currentState == true && visibleState?.targetState == true
                        }
                        
                        itemsIndexed(
                            items = visibleNotifications,
                            key = { _, item -> item.id }) { _, notif ->
                            val visibleState = visibilityStates.getOrPut(notif.id) {
                                MutableTransitionState(true)
                            }

                            val swipeState = rememberSwipeToDismissBoxState(
                                initialValue = SwipeToDismissBoxValue.Settled,
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        // Immediately remove from list to prevent spacing issues
                                        val firebaseId = firebaseIdMap[notif.id]
                                        if (firebaseId != null) {
                                            scope.launch(Dispatchers.IO) {
                                                notificationRepository.deleteNotification(firebaseId)
                                            }
                                        }
                                        // Remove from lists immediately
                                        notifications.remove(notif)
                                        visibilityStates.remove(notif.id)
                                        firebaseIdMap.remove(notif.id)
                                        true
                                    } else false
                                })

                            // Add spacing using padding instead of Arrangement.spacedBy
                            // This prevents gaps when items are removed
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                SwipeToDismissBox(
                                    state = swipeState,
                                    enableDismissFromEndToStart = true,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {}) {
                                    NotificationCard(
                                        item = notif,
                                        unreadBackground = cardColorUnread,
                                        readBackground = cardColorRead,
                                        textColor = textColor,
                                        subtitleColor = subtitleColor,
                                        onClick = {
                                            val idx =
                                                notifications.indexOfFirst { it.id == notif.id }
                                            if (idx != -1 && !notifications[idx].isRead) {
                                                notifications[idx] =
                                                    notifications[idx].copy(isRead = true)
                                                // Mark as read in Firebase
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
        animationSpec = tween(300),
        label = "bgColorAnim"
    )
    val iconBackgroundColor by animateColorAsState(
        targetValue = if (item.isRead) readBackground else unreadBackground,
        animationSpec = tween(300),
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