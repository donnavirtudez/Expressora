package com.example.expressora.dashboard.admin.notification

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
import com.example.expressora.components.admin_bottom_nav.BottomNav2
import com.example.expressora.components.top_nav.TopNav
import com.example.expressora.dashboard.admin.analytics.AnalyticsDashboardActivity
import com.example.expressora.dashboard.admin.communityspacemanagement.CommunitySpaceManagementActivity
import com.example.expressora.dashboard.admin.learningmanagement.LearningManagementActivity
import com.example.expressora.dashboard.admin.quizmanagement.QuizManagementActivity
import com.example.expressora.dashboard.admin.settings.AdminSettingsActivity
import com.example.expressora.ui.theme.InterFontFamily
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Validate role before showing screen - redirect to login if not admin role
        RoleValidationUtil.validateRoleAndRedirect(this, "admin") { isValid ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bgColor = Color(0xFFF8F8F8)
    val cardColorUnread = Color(0xFFFFF4C2)
    val cardColorRead = Color.White
    val textColor = Color.Black
    val subtitleColor = Color(0xFF666666)

    val notifications = remember {
        mutableStateListOf(
            NotificationItem(
                1,
                "Jennie Kim replied to your post",
                "Calling all the PRETTY GIRLS",
                "2h ago",
                R.drawable.sample_profile
            ),
            NotificationItem(2, "Achievement", "You completed 10 lessons!", "3d ago"),
        )
    }

    val visibilityStates = remember {
        mutableStateMapOf<Int, MutableTransitionState<Boolean>>().apply {
            notifications.forEach { notif ->
                this[notif.id] = MutableTransitionState(true)
            }
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val listState = rememberLazyListState()

    fun fetchNewNotifications() {
        scope.launch {
            isRefreshing = true
            delay(1500)
            val newId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
            val newNotif = NotificationItem(
                id = newId,
                title = "New Notification #$newId",
                message = "This is a fresh notification.",
                time = "Just now"
            )
            notifications.add(0, newNotif)
            visibilityStates[newId] = MutableTransitionState(true)
            isRefreshing = false
            listState.scrollToItem(0)
        }
    }

    Scaffold(topBar = {
        TopNav(notificationCount = notifications.count { !it.isRead }, onProfileClick = {
            context.startActivity(
                Intent(
                    context, AdminSettingsActivity::class.java
                )
            )
        }, onTranslateClick = {
            context.startActivity(
                Intent(
                    context, CommunitySpaceManagementActivity::class.java
                )
            )
        }, onNotificationClick = {
            { /* already in admin notification */ }
        })
    }, bottomBar = {
        BottomNav2(onLearnClick = {
            context.startActivity(
                Intent(
                    context, LearningManagementActivity::class.java
                )
            )
        }, onAnalyticsClick = {
            context.startActivity(
                Intent(
                    context, AnalyticsDashboardActivity::class.java
                )
            )
        }, onQuizClick = {
            context.startActivity(
                Intent(
                    context, QuizManagementActivity::class.java
                )
            )
        })
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
                                    // Collect all IDs first to avoid concurrent modification
                                    val allIds = notifications.map { it.id }.toList()
                                    
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
                                    
                                    // Safely clear all at once
                                    notifications.clear()
                                    visibilityStates.clear()
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
                if (notifications.isEmpty()) {
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
                                    // Instant confirmation - no delay, immediate response
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        // Trigger exit animation instantly - zero delay
                                        visibleState.targetState = false
                                        true // Confirm immediately for instant response
                                    } else {
                                        false
                                    }
                                })

                            // Remove item after exit animation completes smoothly
                            if (!visibleState.currentState && !visibleState.targetState) {
                                LaunchedEffect(notif.id) {
                                    // Ultra-fast animation completion (120ms for snappier feel)
                                    delay(120)
                                    // Instant removal - no blocking operations
                                    if (notifications.contains(notif)) {
                                        notifications.remove(notif)
                                        visibilityStates.remove(notif.id)
                                    }
                                }
                            }

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
                                            // Instant mark as read - no delay
                                            val idx = notifications.indexOfFirst { it.id == notif.id }
                                            if (idx != -1 && !notifications[idx].isRead) {
                                                // Update state immediately for instant UI feedback
                                                notifications[idx] = notifications[idx].copy(isRead = true)
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