package com.example.expressora.dashboard.user.quiz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.expressora.components.top_nav.TopNav
import com.example.expressora.components.bottom_nav.BottomNav
import com.example.expressora.dashboard.user.community_space.CommunitySpaceActivity
import com.example.expressora.dashboard.user.learn.LearnActivity
import com.example.expressora.dashboard.user.notification.NotificationActivity
import com.example.expressora.dashboard.user.profile.ProfileActivity
import com.example.expressora.dashboard.user.translation.TranslationActivity
import com.example.expressora.ui.theme.InterFontFamily

class QuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizDifficultyScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDifficultyScreen() {
    val context = LocalContext.current

    val bgColor = Color(0xFFF8F8F8)
    val primaryColor = Color(0xFFFACC15)
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

        var selectedDifficulty by remember { mutableStateOf("") }

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
                text = "Start Quiz",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = textColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose your difficulty level",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                DifficultyCard(
                    label = "🟢 Easy",
                    isSelected = selectedDifficulty == "Easy",
                    onClick = { selectedDifficulty = "Easy" },
                    baseColor = Color(0xFFDFF8E2),
                    gradientColors = listOf(Color(0xFFA9E5A0), Color(0xFFDFF8E2)),
                    borderColor = Color(0xFF79C36A),
                    selectedElevation = 12.dp
                )
                DifficultyCard(
                    label = "🟡 Medium",
                    isSelected = selectedDifficulty == "Medium",
                    onClick = { selectedDifficulty = "Medium" },
                    baseColor = Color(0xFFFFF6D1),
                    gradientColors = listOf(Color(0xFFFBEA87), Color(0xFFFFF6D1)),
                    borderColor = Color(0xFFFACC15),
                    selectedElevation = 12.dp
                )
                DifficultyCard(
                    label = "🔴 Hard",
                    isSelected = selectedDifficulty == "Hard",
                    onClick = { selectedDifficulty = "Hard" },
                    baseColor = Color(0xFFFFE6E6),
                    gradientColors = listOf(Color(0xFFF79595), Color(0xFFFFE6E6)),
                    borderColor = Color(0xFFD85050),
                    selectedElevation = 12.dp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {

                },
                enabled = selectedDifficulty.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFFF3E2A9),
                    disabledContentColor = Color(0xFF999999)
                )
            ) {
                Text(
                    text = "Start Quiz",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
fun DifficultyCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    baseColor: Color,
    gradientColors: List<Color>,
    borderColor: Color,
    selectedElevation: Dp = 8.dp
) {
    val backgroundBrush = if (isSelected) {
        Brush.linearGradient(
            colors = gradientColors,
            start = Offset(0f, 0f),
            end = Offset(300f, 300f),
            tileMode = TileMode.Clamp
        )
    } else {
        Brush.linearGradient(
            colors = listOf(baseColor, baseColor)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush = backgroundBrush)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.shadow(selectedElevation, RoundedCornerShape(16.dp))
                else Modifier.border(
                    1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)
                )
            )
            .padding(start = 24.dp), contentAlignment = Alignment.CenterStart) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontFamily = InterFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) borderColor else Color(0xFF333333)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuizDifficultyScreenPreview() {
    QuizDifficultyScreen()
}
