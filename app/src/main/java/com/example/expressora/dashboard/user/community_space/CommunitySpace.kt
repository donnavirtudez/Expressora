package com.example.expressora.dashboard.user.community_space

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.expressora.R
import com.example.expressora.components.bottom_nav.BottomNav
import com.example.expressora.components.top_nav.TopNav
import com.example.expressora.components.top_nav2.TopTabNav2
import com.example.expressora.dashboard.user.learn.LearnActivity
import com.example.expressora.dashboard.user.notification.NotificationActivity
import com.example.expressora.dashboard.user.profile.ProfileActivity
import com.example.expressora.dashboard.user.quiz.QuizActivity
import com.example.expressora.dashboard.user.translation.TranslationActivity
import com.google.accompanist.swiperefresh.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.example.expressora.ui.theme.InterFontFamily

class CommunitySpaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CommunitySpaceScreen() }
    }
}

data class Comment(
    val id: Int, val author: String, val avatarRes: Int, val message: String
)

data class Post(
    val id: Int,
    val author: String,
    val avatarRes: Int,
    val content: String,
    val imageUri: Uri? = null,
    val videoUri: Uri? = null,
    var likes: Int = 0,
    var comments: MutableList<Comment> = mutableListOf(),
    var isLiked: Boolean = false,
    var showComments: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

fun getTimeAgo(time: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - time
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "$days day${if (days > 1) "s" else ""} ago"
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitySpaceScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bgColor = Color(0xFFF8F8F8)
    val subtitleColor = Color(0xFF666666)

    var posts by remember {
        mutableStateOf(
            listOf(
                Post(
                    id = 1,
                    author = "Taylor Swift",
                    avatarRes = R.drawable.taylor_swift,
                    content = "Yall be doing the most",
                    likes = 2,
                    comments = mutableListOf(
                        Comment(1, "Hyein Lee", R.drawable.sample_profile2, "Problema mo, bakla?")
                    ),
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 50
                ), Post(
                    id = 2,
                    author = "Azealia Banks",
                    avatarRes = R.drawable.azealia_banks,
                    content = "Iggy Azalea is like my albino child I gave birth to in a pre-historic African village during Pangea",
                    imageUri = Uri.parse("https://static.scientificamerican.com/sciam/cache/file/3A647477-C180-4D23-B4265C83F4906F2A_source.jpg?w=600"),
                    likes = 5,
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5
                ), Post(
                    id = 3,
                    author = "LOONA",
                    avatarRes = R.drawable.loona,
                    content = "Bye",
                    videoUri = Uri.parse("https://youtu.be/_EEo-iE5u_A?si=a97B0OU3m1Pbn-Sc"),
                    likes = 1,
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24
                )
            )
        )
    }

    var searchQuery by remember { mutableStateOf("") }

    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val listState = rememberLazyListState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPostText by remember { mutableStateOf("") }

    var previewImageUri by remember { mutableStateOf<Uri?>(null) }
    var previewVideoUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val newId = (posts.maxOfOrNull { it.id } ?: 0) + 1
            val newPost = Post(
                id = newId,
                author = "Jennie Kim",
                avatarRes = R.drawable.sample_profile,
                content = newPostText.ifBlank { "Shared an image" },
                imageUri = it
            )
            posts = listOf(newPost) + posts
            newPostText = ""
            showCreateDialog = false
            scope.launch { listState.animateScrollToItem(0) }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val newId = (posts.maxOfOrNull { it.id } ?: 0) + 1
            val newPost = Post(
                id = newId,
                author = "Jennie Kim",
                avatarRes = R.drawable.sample_profile,
                content = newPostText.ifBlank { "Shared a video" },
                videoUri = it
            )
            posts = listOf(newPost) + posts
            newPostText = ""
            showCreateDialog = false
            scope.launch { listState.animateScrollToItem(0) }
        }
    }

    fun refreshPosts() {
        scope.launch {
            isRefreshing = true
            delay(1200)
            val newId = (posts.maxOfOrNull { it.id } ?: 0) + 1
            val newPost = Post(
                id = newId,
                author = "Expressora",
                avatarRes = R.drawable.expressora_logo,
                content = "Here's a new post from refresh!",
                timestamp = System.currentTimeMillis()
            )
            posts = listOf(newPost) + posts
            isRefreshing = false
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(topBar = {
        Column {
            TopNav(notificationCount = 2, onProfileClick = {
                context.startActivity(Intent(context, ProfileActivity::class.java))
            }, onTranslateClick = {
                context.startActivity(Intent(context, CommunitySpaceActivity::class.java))
            }, onNotificationClick = {
                context.startActivity(Intent(context, NotificationActivity::class.java))
            })

            var selectedTab by remember { mutableStateOf(0) }
            TopTabNav2(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search posts", color = Color(0xFF666666)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search, contentDescription = "Search", tint = Color.Black
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White, RoundedCornerShape(50))
                    .shadow(2.dp, RoundedCornerShape(50)),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color(0xFF666666),
                    cursorColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }
    }, bottomBar = {
        BottomNav(onLearnClick = {
            context.startActivity(Intent(context, LearnActivity::class.java))
        }, onCameraClick = {
            context.startActivity(Intent(context, TranslationActivity::class.java))
        }, onQuizClick = {
            context.startActivity(Intent(context, QuizActivity::class.java))
        })
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = Color(0xFFFACC15),
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Post", tint = Color.Black)
        }
    }) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { refreshPosts() },
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(paddingValues)
        ) {
            val filteredPosts = if (searchQuery.isBlank()) posts else posts.filter {
                it.content.contains(searchQuery, ignoreCase = true) || it.author.contains(
                    searchQuery, ignoreCase = true
                )
            }

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    top = 0.dp, bottom = 12.dp, start = 24.dp, end = 24.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                if (searchQuery.isNotBlank() && filteredPosts.isEmpty()) {
                    item {
                        Text(
                            "No posts found.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF666666)
                        )
                    }
                }

                itemsIndexed(filteredPosts, key = { _, post -> post.id }) { index, post ->
                    PostCard(
                        post = post,
                        subtitleColor = subtitleColor,
                        onLikeToggle = {
                            posts = posts.map {
                                if (it.id == post.id) {
                                    val liked = !it.isLiked
                                    it.copy(
                                        isLiked = liked,
                                        likes = if (liked) it.likes + 1 else it.likes - 1
                                    )
                                } else it
                            }
                        },
                        onCommentToggle = {
                            posts = posts.map {
                                if (it.id == post.id) it.copy(showComments = !it.showComments) else it
                            }
                        },
                        onAddComment = { message ->
                            posts = posts.map {
                                if (it.id == post.id) {
                                    it.copy(
                                        comments = (it.comments + Comment(
                                            it.comments.size + 1,
                                            "Jennie Kim",
                                            R.drawable.sample_profile,
                                            message
                                        )).toMutableList()
                                    )
                                } else it
                            }
                        },
                        onImageClick = { previewImageUri = it },
                        onVideoClick = { previewVideoUri = it },
                        onNext = {
                            scope.launch {
                                val target = index + 1
                                if (target < filteredPosts.size) listState.animateScrollToItem(
                                    target
                                )
                            }
                        })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePostDialog(
            newPostText = newPostText,
            onTextChange = { newPostText = it },
            onDismiss = { showCreateDialog = false },
            onPost = {
                if (newPostText.isNotBlank()) {
                    val newId = (posts.maxOfOrNull { it.id } ?: 0) + 1
                    val newPost = Post(
                        id = newId,
                        author = "Jennie Kim",
                        avatarRes = R.drawable.sample_profile,
                        content = newPostText
                    )
                    posts = listOf(newPost) + posts
                    newPostText = ""
                    showCreateDialog = false
                    scope.launch { listState.animateScrollToItem(0) }
                }
            },
            onPickImage = { imagePicker.launch("image/*") },
            onPickVideo = { videoPicker.launch("video/*") })
    }

    previewImageUri?.let { uri ->
        Dialog(onDismissRequest = { previewImageUri = null }) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Preview Image",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    previewVideoUri?.let { uri ->
        Dialog(onDismissRequest = { previewVideoUri = null }) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePostDialog(
    newPostText: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPost: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Create Post",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = newPostText,
                    onValueChange = onTextChange,
                    label = { Text("What’s on your mind?", color = Color(0xFF666666)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color(0xFF666666),
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color(0xFF666666)
                    ),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(12.dp))

                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(onClick = onPickImage) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Add Image",
                                tint = Color.Black
                            )
                        }
                        IconButton(onClick = onPickVideo) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = "Add Video",
                                tint = Color.Black
                            )
                        }
                    }
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text(
                                "Cancel", color = Color(0xFF666666)
                            )
                        }
                        Button(
                            onClick = onPost, colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFACC15), contentColor = Color.Black
                            )
                        ) { Text("Post") }
                    }
                }
            }
        }
    }
}


@Composable
fun PostCard(
    post: Post,
    subtitleColor: Color,
    onLikeToggle: () -> Unit,
    onCommentToggle: () -> Unit,
    onAddComment: (String) -> Unit,
    onImageClick: (Uri) -> Unit,
    onVideoClick: (Uri) -> Unit,
    onNext: () -> Unit
) {
    var newComment by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = post.avatarRes),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        post.author,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        getTimeAgo(post.timestamp),
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        color = subtitleColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val maxLinesWhenCollapsed = 3
            Text(
                text = post.content,
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                color = subtitleColor,
                maxLines = if (expanded) Int.MAX_VALUE else maxLinesWhenCollapsed,
                overflow = TextOverflow.Ellipsis
            )

            val isLongContent = post.content.lineSequence().joinToString(" ").length > 120
            if (isLongContent) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (expanded) "Show less" else "See more",
                        modifier = Modifier.clickable { expanded = !expanded },
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF666666)
                    )

                    Text(
                        text = "Next",
                        modifier = Modifier.clickable { onNext() },
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF666666)
                    )
                }
            }

            post.imageUri?.let {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = it,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(it) },
                    contentScale = ContentScale.Crop
                )
            }

            post.videoUri?.let {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .clickable { onVideoClick(it) }, contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeToggle) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        modifier = Modifier.size(20.dp),
                        tint = if (post.isLiked) Color(0xFFFACC15) else Color.Black
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "${post.likes}",
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    color = subtitleColor
                )

                Spacer(Modifier.width(16.dp))
                IconButton(onClick = onCommentToggle) {
                    Icon(
                        imageVector = Icons.Default.ModeComment,
                        contentDescription = "Comment",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Black
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "${post.comments.size}",
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    color = subtitleColor
                )
            }

            if (post.showComments) {
                Spacer(Modifier.height(8.dp))
                Column {
                    post.comments.forEach { comment ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = comment.avatarRes),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    comment.author,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp
                                )
                                Text(
                                    comment.message,
                                    fontSize = 13.sp,
                                    fontFamily = InterFontFamily,
                                    color = subtitleColor
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newComment,
                            onValueChange = { newComment = it },
                            placeholder = { Text("Write a reply...", color = Color(0xFF666666)) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color(0xFF666666),
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color(0xFF666666)
                            )
                        )
                        IconButton(
                            onClick = {
                                if (newComment.isNotBlank()) {
                                    onAddComment(newComment)
                                    newComment = ""
                                }
                            }) {
                            Icon(
                                Icons.Default.Send, contentDescription = "Reply", tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCommunitySpaceScreen() {
    CommunitySpaceScreen()
}
