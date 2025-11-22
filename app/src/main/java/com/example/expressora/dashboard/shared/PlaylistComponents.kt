package com.example.expressora.dashboard.shared

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.expressora.ui.theme.InterFontFamily

// Import data classes - both user and admin have identical definitions
// Accept either user or admin PlaylistItem/VideoItem (they're the same via typealias)
typealias PlaylistItem = com.example.expressora.dashboard.user.tutorial.PlaylistItem
typealias VideoItem = com.example.expressora.dashboard.user.tutorial.VideoItem

@Composable
fun PlaylistTutorialCard(
    playlist: PlaylistItem,
    onExpandClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onVideoClick: (VideoItem) -> Unit
) {
    val expressoraGold = Color(0xFFFACC15)
    val expressoraGoldLight = Color(0xFFFFF4C2)
    val cardBgColor = if (playlist.isExpanded) Color.White else expressoraGoldLight
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {
            // Playlist Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onPlaylistClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (playlist.thumbnailUrl != null) {
                        AsyncImage(
                            model = playlist.thumbnailUrl,
                            contentDescription = playlist.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray)
                        )
                    }
                    // Playlist icon overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x99000000)),
                                    startY = 0f,
                                    endY = 300f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = "Playlist",
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .padding(12.dp)
                        )
                    }
                }
                
                // Playlist Info
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = playlist.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${playlist.itemCount} videos",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        fontFamily = InterFontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playlist.description,
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        fontFamily = InterFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Expand/Collapse Button
                IconButton(
                    onClick = { onExpandClick() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (playlist.isExpanded) expressoraGold else Color.White,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (playlist.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (playlist.isExpanded) "Collapse" else "Expand",
                        tint = if (playlist.isExpanded) Color.Black else expressoraGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Expanded Content - Playlist Videos
            AnimatedVisibility(
                visible = playlist.isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300),
                    expandFrom = Alignment.Top
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300),
                    shrinkTowards = Alignment.Top
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F8F8))
                ) {
                    if (playlist.playlistVideos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading videos...",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontFamily = InterFontFamily
                            )
                        }
                    } else {
                        playlist.playlistVideos.forEachIndexed { index, video ->
                            PlaylistVideoItem(
                                video = video,
                                onClick = { onVideoClick(video) },
                                isLast = index == playlist.playlistVideos.size - 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistVideoItem(
    video: VideoItem,
    onClick: () -> Unit,
    isLast: Boolean
) {
    val cardBgColor = if (video.isWatched) Color.White else Color(0xFFFFF4C2)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (video.thumbnailUrl != null) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000)),
                                startY = 0f,
                                endY = 200f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(8.dp)
                    )
                }
            }
            
            // Video Info
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = video.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.description,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

