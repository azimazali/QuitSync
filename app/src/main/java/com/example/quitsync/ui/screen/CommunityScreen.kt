package com.example.quitsync.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.viewmodel.CommunityViewModel
import com.example.quitsync.viewmodel.CommunityUiState
import com.example.quitsync.model.Post
import com.example.quitsync.model.Comment

@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel(),
    onNavigateToSettings: () -> Unit
) {
    val posts by viewModel.posts
    val commentsMap by viewModel.commentsMap
    val currentUserId = viewModel.currentUserId
    val uiState by viewModel.uiState
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var postToEdit by remember { mutableStateOf<Post?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is CommunityUiState.Error) {
            snackbarHostState.showSnackbar((uiState as CommunityUiState.Error).message)
            viewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreatePostDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (posts.isEmpty() && uiState is CommunityUiState.Idle) {
                    item {
                        Box(modifier = Modifier.fillParentMaxHeight(0.7f), contentAlignment = Alignment.Center) {
                            Text("No posts yet. Be the first to share!", color = Color.Gray)
                        }
                    }
                }

                items(posts) { post ->
                    PostItem(
                        post = post,
                        comments = commentsMap[post.id] ?: emptyList(),
                        isOwnPost = post.userId == currentUserId,
                        currentUserId = currentUserId,
                        onEdit = { postToEdit = it },
                        onDelete = { viewModel.deletePost(it.id) },
                        onLike = { viewModel.toggleLike(post) },
                        onAddComment = { content -> viewModel.addComment(post.id, content) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (uiState is CommunityUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showCreatePostDialog) {
        PostDialog(
            title = "Create Post",
            onDismiss = { showCreatePostDialog = false },
            onSave = { title, desc ->
                viewModel.createPost(title, desc)
                showCreatePostDialog = false
            }
        )
    }

    if (postToEdit != null) {
        PostDialog(
            title = "Edit Post",
            initialTitle = postToEdit!!.title,
            initialDescription = postToEdit!!.description,
            onDismiss = { postToEdit = null },
            onSave = { title, desc ->
                viewModel.updatePost(postToEdit!!.id, title, desc)
                postToEdit = null
            }
        )
    }
}

@Composable
fun PostItem(
    post: Post,
    comments: List<Comment>,
    isOwnPost: Boolean,
    currentUserId: String?,
    onEdit: (Post) -> Unit,
    onDelete: (Post) -> Unit,
    onLike: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val isLiked = currentUserId != null && post.likedBy.contains(currentUserId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = post.userName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(text = post.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                if (isOwnPost) {
                    Row {
                        IconButton(onClick = { onEdit(post) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDelete(post) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = Color.Red)
                        }
                    }
                }
            }

            Text(text = post.description, modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else LocalContentColor.current
                    )
                }
                Text(text = "${post.likedBy.size} likes", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.width(16.dp))

                Icon(imageVector = Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${comments.size} comments", style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            comments.forEach { comment ->
                Column(modifier = Modifier.padding(top = 8.dp, start = 8.dp)) {
                    Text(text = comment.userName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(text = comment.content, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (showCommentInput) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                                showCommentInput = false
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                        }
                    }
                )
            } else {
                TextButton(onClick = { showCommentInput = true }) {
                    Text("Add Comment", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun PostDialog(
    title: String,
    initialTitle: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var postTitle by remember { mutableStateOf(initialTitle) }
    var postDesc by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = postTitle,
                    onValueChange = { postTitle = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. 10 days smoke free!") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = postDesc,
                    onValueChange = { postDesc = it },
                    label = { Text("Description") },
                    placeholder = { Text("Share your story...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (postTitle.isNotBlank() && postDesc.isNotBlank()) onSave(postTitle, postDesc) },
                enabled = postTitle.isNotBlank() && postDesc.isNotBlank()
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
