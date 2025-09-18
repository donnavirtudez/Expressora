package com.example.expressora.dashboard.user.learn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressora.components.top_nav.TopNav
import com.example.expressora.components.bottom_nav.BottomNav
import com.example.expressora.dashboard.user.community_space.CommunitySpaceActivity
import com.example.expressora.dashboard.user.notification.NotificationActivity
import com.example.expressora.dashboard.user.profile.ProfileActivity
import com.example.expressora.dashboard.user.quiz.QuizActivity
import com.example.expressora.dashboard.user.translation.TranslationActivity
import com.example.expressora.ui.theme.InterFontFamily

class LearnActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen() {
    val context = LocalContext.current

    val bgColor = Color(0xFFF8F8F8)
    val primaryYellow = Color(0xFFFACC15)
    val textColor = Color.Black

    Scaffold(topBar = {
        TopNav(notificationCount = 2, onProfileClick = {
            context.startActivity(Intent(context, ProfileActivity::class.java))
        }, onTranslateClick = {
            context.startActivity(
                Intent(
                    context, CommunitySpaceActivity::class.java
                )
            )
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
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            Text(
                text = "Welcome back",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = textColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "What would you like to learn today?",
                fontSize = 16.sp,
                color = Color.DarkGray,
                fontFamily = InterFontFamily
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LearningCard(title = "Basic Phrases", color = primaryYellow, onClick = { })
                LearningCard(
                    title = "Daily Conversations", color = Color(0xFFE0E7FF), onClick = { })
                LearningCard(title = "Grammar Rules", color = Color(0xFFFFE4E1), onClick = { })
            }
        }
    }
}

@Composable
fun LearningCard(title: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            fontFamily = InterFontFamily
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LearnScreenPreview() {
    LearnScreen()
}
