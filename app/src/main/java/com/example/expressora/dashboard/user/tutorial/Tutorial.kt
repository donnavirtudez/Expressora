package com.example.expressora.dashboard.user.tutorial

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.expressora.utils.RoleValidationUtil
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressora.R
import com.example.expressora.components.top_nav.TopNav
import com.example.expressora.components.top_nav.rememberNotificationCount
import com.example.expressora.components.user_bottom_nav.BottomNav
import com.example.expressora.components.user_top_nav2.TopTabNav2
import com.example.expressora.dashboard.user.learn.LearnActivity
import com.example.expressora.dashboard.user.notification.NotificationActivity
import com.example.expressora.dashboard.user.quiz.QuizActivity
import com.example.expressora.dashboard.user.settings.SettingsActivity
import com.example.expressora.dashboard.user.translation.TranslationActivity
import com.example.expressora.ui.theme.InterFontFamily
import com.example.expressora.backend.YouTubeService
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import android.util.Log

class TutorialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Validate role before showing screen - redirect to login if not user role
        RoleValidationUtil.validateRoleAndRedirect(this, "user") { isValid ->
            if (!isValid) {
                return@validateRoleAndRedirect // Will redirect to login
            }
            
            // Show screen only if role is valid
            setContent {
                TutorialScreen()
            }
        }
    }
}

data class VideoItem(
    val id: String, // Changed to String to support YouTube video IDs
    val title: String,
    val description: String,
    val thumbnailUrl: String? = null, // YouTube thumbnail URL
    val thumbnailRes: Int? = null, // Local resource (fallback)
    val videoUrl: String,
    var isWatched: Boolean = false,
    val publishedAt: String? = null
)

data class PlaylistItem(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String? = null,
    val playlistUrl: String,
    val itemCount: Int,
    val publishedAt: String? = null,
    var isExpanded: Boolean = false,
    var playlistVideos: List<VideoItem> = emptyList()
)

sealed class TutorialContent {
    data class Video(val video: VideoItem) : TutorialContent()
    data class Playlist(val playlist: PlaylistItem) : TutorialContent()
}

@Composable
fun VideoTutorialCard(
    title: String,
    description: String,
    thumbnailUrl: String? = null,
    thumbnailRes: Int? = null,
    isWatched: Boolean,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit = {}
) {
    val cardBgColor = if (isWatched) Color.White else Color(0xFFFFF4C2)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 14.dp))
            ) {
                // Use AsyncImage for YouTube thumbnails, fallback to local resource
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = if (thumbnailRes != null) {
                            painterResource(id = thumbnailRes)
                        } else {
                            null
                        }
                    )
                } else if (thumbnailRes != null) {
                    Image(
                        painter = painterResource(id = thumbnailRes),
                        contentDescription = title,
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
                                endY = 300f
                            )
                        ), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(12.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    fontFamily = InterFontFamily,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.description,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bgColor = Color(0xFFF8F8F8)
    val youtubeService = remember { YouTubeService() }

    val videos = remember { mutableStateListOf<VideoItem>() }
    val playlists = remember { mutableStateListOf<PlaylistItem>() }
    val contentItems = remember { mutableStateListOf<TutorialContent>() }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val listState = rememberLazyListState()

    // Get user email for user-specific watched videos
    val sharedPref = remember { context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE) }
    val userEmail = remember { sharedPref.getString("user_email", "") ?: "" }

    // Load watched videos from SharedPreferences (user-specific)
    fun loadWatchedVideos(): Set<String> {
        if (userEmail.isEmpty()) return emptySet()
        val watchedPref = context.getSharedPreferences("watched_videos", android.content.Context.MODE_PRIVATE)
        val key = "watched_video_ids_$userEmail"
        return watchedPref.getStringSet(key, emptySet()) ?: emptySet()
    }

    // Save watched video to SharedPreferences (user-specific)
    fun saveWatchedVideo(videoId: String) {
        if (userEmail.isEmpty()) return
        val watchedPref = context.getSharedPreferences("watched_videos", android.content.Context.MODE_PRIVATE)
        val key = "watched_video_ids_$userEmail"
        val watchedIds = watchedPref.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        watchedIds.add(videoId)
        watchedPref.edit().putStringSet(key, watchedIds).apply()
    }

    // Fetch videos and playlists from YouTube API (with silent background refresh option)
    fun fetchVideos(isBackgroundRefresh: Boolean = false) {
        scope.launch {
            if (!isBackgroundRefresh) {
                isRefreshing = true
                isLoading = true
            }
            errorMessage = null
            
            try {
                // Fetch both videos and playlists in parallel
                val videosResult = youtubeService.getChannelVideos(maxResults = 50)
                val playlistsResult = youtubeService.getChannelPlaylists(maxResults = 50)
                
                videosResult.onSuccess { response ->
                    if (response.success && response.videos != null) {
                        // Load watched videos from SharedPreferences
                        val watchedIds = loadWatchedVideos()
                        
                        // Convert YouTube videos to VideoItem with edge case handling
                        val newVideos = response.videos
                            .filter { it.videoId.isNotBlank() && it.title.isNotBlank() } // Filter invalid videos
                            .mapNotNull { ytVideo ->
                                try {
                                    VideoItem(
                                        id = ytVideo.videoId,
                                        title = ytVideo.title.take(200), // Limit title length
                                        description = (ytVideo.description.take(100) + if (ytVideo.description.length > 100) "..." else "").take(150),
                                        thumbnailUrl = ytVideo.thumbnailUrl.takeIf { it.isNotBlank() },
                                        videoUrl = "https://www.youtube.com/watch?v=${ytVideo.videoId}",
                                        isWatched = watchedIds.contains(ytVideo.videoId),
                                        publishedAt = ytVideo.publishedAt
                                    )
                                } catch (e: Exception) {
                                    null // Skip invalid videos
                                }
                            }
                        
                        videos.clear()
                        videos.addAll(newVideos)
                    } else if (!isBackgroundRefresh && response.message != null) {
                        errorMessage = response.message
                    }
                }.onFailure { error ->
                    if (!isBackgroundRefresh) {
                        errorMessage = error.message ?: "Failed to fetch videos"
                    }
                }
                
                playlistsResult.onSuccess { playlistList ->
                    val newPlaylists = playlistList
                        .filter { it.playlistId.isNotBlank() && it.title.isNotBlank() } // Filter invalid playlists
                        .mapNotNull { ytPlaylist ->
                            try {
                                PlaylistItem(
                                    id = ytPlaylist.playlistId,
                                    title = ytPlaylist.title.take(200), // Limit title length
                                    description = (ytPlaylist.description.take(100) + if (ytPlaylist.description.length > 100) "..." else "").take(150),
                                    thumbnailUrl = ytPlaylist.thumbnailUrl.takeIf { it.isNotBlank() },
                                    playlistUrl = ytPlaylist.playlistUrl,
                                    itemCount = ytPlaylist.itemCount.coerceAtLeast(0), // Ensure non-negative
                                    publishedAt = ytPlaylist.publishedAt,
                                    isExpanded = false,
                                    playlistVideos = emptyList()
                                )
                            } catch (e: Exception) {
                                null // Skip invalid playlists
                            }
                        }
                    
                    playlists.clear()
                    playlists.addAll(newPlaylists)
                }.onFailure { error ->
                    // Playlist fetch failure is not critical, just log
                    if (!isBackgroundRefresh) {
                        android.util.Log.w("TutorialScreen", "Failed to fetch playlists: ${error.message}")
                    }
                }
                
                // Combine videos and playlists into content items (playlists first, then videos)
                contentItems.clear()
                playlists.forEach { playlist ->
                    contentItems.add(TutorialContent.Playlist(playlist))
                }
                videos.forEach { video ->
                    contentItems.add(TutorialContent.Video(video))
                }
                
                // Only set error if we have no content and it's not a background refresh
                if (!isBackgroundRefresh && contentItems.isEmpty() && errorMessage == null) {
                    errorMessage = "No videos or playlists available"
                } else if (contentItems.isNotEmpty()) {
                    errorMessage = null
                }
                
            } catch (e: Exception) {
                if (!isBackgroundRefresh) {
                    errorMessage = e.message ?: "Failed to fetch content"
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (!isBackgroundRefresh) {
                    isRefreshing = false
                    isLoading = false
                }
            }
        }
    }
    
    // Load playlist videos when expanded
    fun loadPlaylistVideos(playlist: PlaylistItem) {
        scope.launch {
            try {
                val result = youtubeService.getPlaylistVideos(playlist.id, maxResults = 50)
                result.onSuccess { playlistVideosList ->
                    val watchedIds = loadWatchedVideos()
                    val videoItems = playlistVideosList
                        .filter { it.videoId.isNotBlank() && it.title.isNotBlank() } // Filter invalid videos
                        .mapNotNull { ytVideo ->
                            try {
                                VideoItem(
                                    id = ytVideo.videoId,
                                    title = ytVideo.title.take(200),
                                    description = (ytVideo.description.take(80) + if (ytVideo.description.length > 80) "..." else "").take(150),
                                    thumbnailUrl = ytVideo.thumbnailUrl.takeIf { it.isNotBlank() },
                                    videoUrl = "https://www.youtube.com/watch?v=${ytVideo.videoId}",
                                    isWatched = watchedIds.contains(ytVideo.videoId),
                                    publishedAt = ytVideo.publishedAt
                                )
                            } catch (e: Exception) {
                                null // Skip invalid videos
                            }
                        }
                    
                    // Update playlist in the list
                    val index = playlists.indexOfFirst { it.id == playlist.id }
                    if (index != -1) {
                        playlists[index] = playlists[index].copy(
                            isExpanded = true,
                            playlistVideos = videoItems
                        )
                        
                        // Update in contentItems
                        val contentIndex = contentItems.indexOfFirst { 
                            it is TutorialContent.Playlist && it.playlist.id == playlist.id 
                        }
                        if (contentIndex != -1) {
                            contentItems[contentIndex] = TutorialContent.Playlist(playlists[index])
                        }
                    }
                }.onFailure { error ->
                    // Handle error loading playlist videos
                    val index = playlists.indexOfFirst { it.id == playlist.id }
                    if (index != -1) {
                        // Keep expanded state but show empty list
                        playlists[index] = playlists[index].copy(
                            isExpanded = true,
                            playlistVideos = emptyList()
                        )
                        val contentIndex = contentItems.indexOfFirst { 
                            it is TutorialContent.Playlist && it.playlist.id == playlist.id 
                        }
                        if (contentIndex != -1) {
                            contentItems[contentIndex] = TutorialContent.Playlist(playlists[index])
                        }
                    }
                    Toast.makeText(context, "Failed to load playlist videos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Initial fetch
    LaunchedEffect(Unit) {
        fetchVideos()
    }

    // Background auto-refresh every 5 minutes (silent, no loading indicator)
    LaunchedEffect(Unit) {
        while (true) {
            delay(5 * 60 * 1000) // 5 minutes
            fetchVideos(isBackgroundRefresh = true)
        }
    }

    val userRole = remember { sharedPref.getString("user_role", "user") ?: "user" }
    val notificationCount = rememberNotificationCount(userEmail, userRole)

    Scaffold(topBar = {
        Column {
            TopNav(notificationCount = notificationCount, onProfileClick = {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }, onTranslateClick = {
                { /* already in tutorial */ }
            }, onNotificationClick = {
                context.startActivity(Intent(context, NotificationActivity::class.java))
            })
            // Always set selectedTab to 1 for ASL/FSL Video screen
            val selectedTab = 1
            TopTabNav2(selectedTab = selectedTab, onTabSelected = { /* Tab selection handled by navigation */ })
        }
    }, bottomBar = {
        BottomNav(onLearnClick = {
            context.startActivity(Intent(context, LearnActivity::class.java))
        }, onCameraClick = {
            context.startActivity(Intent(context, TranslationActivity::class.java))
        }, onQuizClick = {
            context.startActivity(Intent(context, QuizActivity::class.java))
        })
    }) { paddingValues ->
        // Show loading spinner on initial load (no swipe refresh)
        if (isLoading && contentItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.Black,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        } else {
            // Show swipe refresh only after initial load
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { fetchVideos(isBackgroundRefresh = false) },
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(paddingValues)
            ) {
                when {
                errorMessage != null && contentItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Error loading videos",
                                fontSize = 16.sp,
                                color = Color.Red,
                                fontFamily = InterFontFamily
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "Unknown error",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontFamily = InterFontFamily
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { fetchVideos() }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                contentItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No videos available",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontFamily = InterFontFamily
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(
                            top = 20.dp, bottom = 20.dp, start = 24.dp, end = 24.dp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor)
                    ) {
                        itemsIndexed(contentItems, key = { _, item -> 
                            when (item) {
                                is TutorialContent.Video -> "video_${item.video.id}"
                                is TutorialContent.Playlist -> "playlist_${item.playlist.id}"
                            }
                        }) { _, content ->
                            when (content) {
                                is TutorialContent.Video -> {
                                    VideoTutorialCard(
                                        title = content.video.title,
                                        description = content.video.description,
                                        thumbnailUrl = content.video.thumbnailUrl,
                                        thumbnailRes = content.video.thumbnailRes,
                                        isWatched = content.video.isWatched,
                                        onClick = {
                                            // Open video in YouTube
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content.video.videoUrl))
                                            context.startActivity(intent)
                                            
                                            // Mark as watched
                                            val idx = videos.indexOfFirst { it.id == content.video.id }
                                            if (idx != -1 && !videos[idx].isWatched) {
                                                saveWatchedVideo(content.video.id)
                                                videos[idx] = videos[idx].copy(isWatched = true)
                                                // Update in contentItems
                                                val contentIdx = contentItems.indexOfFirst { 
                                                    it is TutorialContent.Video && it.video.id == content.video.id 
                                                }
                                                if (contentIdx != -1) {
                                                    contentItems[contentIdx] = TutorialContent.Video(videos[idx])
                                                }
                                            }
                                        },
                                        onDownloadClick = {})
                                }
                                is TutorialContent.Playlist -> {
                                    PlaylistTutorialCard(
                                        playlist = content.playlist,
                                        onExpandClick = {
                                            val idx = playlists.indexOfFirst { it.id == content.playlist.id }
                                            if (idx != -1) {
                                                if (playlists[idx].isExpanded) {
                                                    // Collapse
                                                    playlists[idx] = playlists[idx].copy(isExpanded = false)
                                                } else {
                                                    // Expand and load videos
                                                    loadPlaylistVideos(playlists[idx])
                                                }
                                                // Update in contentItems
                                                val contentIdx = contentItems.indexOfFirst { 
                                                    it is TutorialContent.Playlist && it.playlist.id == content.playlist.id 
                                                }
                                                if (contentIdx != -1) {
                                                    contentItems[contentIdx] = TutorialContent.Playlist(playlists[idx])
                                                }
                                            }
                                        },
                                        onPlaylistClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content.playlist.playlistUrl))
                                            context.startActivity(intent)
                                        },
                                        onVideoClick = { video: VideoItem ->
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.videoUrl))
                                            context.startActivity(intent)
                                            
                                            // Mark as watched
                                            saveWatchedVideo(video.id)
                                            val playlistIdx = playlists.indexOfFirst { it.id == content.playlist.id }
                                            if (playlistIdx != -1) {
                                                val videoIdx = playlists[playlistIdx].playlistVideos.indexOfFirst { it.id == video.id }
                                                if (videoIdx != -1 && !playlists[playlistIdx].playlistVideos[videoIdx].isWatched) {
                                                    val updatedVideos = playlists[playlistIdx].playlistVideos.toMutableList()
                                                    updatedVideos[videoIdx] = updatedVideos[videoIdx].copy(isWatched = true)
                                                    playlists[playlistIdx] = playlists[playlistIdx].copy(playlistVideos = updatedVideos)
                                                    
                                                    // Update in contentItems
                                                    val contentIdx = contentItems.indexOfFirst { 
                                                        it is TutorialContent.Playlist && it.playlist.id == content.playlist.id 
                                                    }
                                                    if (contentIdx != -1) {
                                                        contentItems[contentIdx] = TutorialContent.Playlist(playlists[playlistIdx])
                                                    }
                                                }
                                            }
                                        }
                                    )
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

// Preview removed - Previews with LocalContext don't work well
// Use Android Studio's interactive preview instead