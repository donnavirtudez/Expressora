package com.example.expressora.components.top_nav3

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.expressora.R
import com.example.expressora.dashboard.user.community_space.CommunitySpaceActivity

@Composable
fun TopNav3(
    modifier: Modifier = Modifier, onTranslateClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color(0xFFF8F8F8)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0xFFF8F8F8))
                .clickable { onTranslateClick() }, contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.expressora_logo),
                contentDescription = "Expressora Logo",
                modifier = Modifier.size(37.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopNavBar3Preview() {
    val context = LocalContext.current

    TopNav3(
        onTranslateClick = {
            context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
        })
}
