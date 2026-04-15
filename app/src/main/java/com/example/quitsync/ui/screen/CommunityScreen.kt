package com.example.quitsync.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.viewmodel.CommunityViewModel
import com.example.quitsync.model.Post
import com.example.quitsync.model.Comment

@Composable
fun CommunityScreen(viewModel: CommunityViewModel = viewModel()) {
    val posts by viewModel.posts
    val commentsMap by viewModel.commentsMap
    var showCreatePostDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreatePostDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Community",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            items(posts) { post ->
                PostItem(
                    post = post,
                    comments = commentsMap[post.id] ?: emptyList(),
                    onAddComment = { content -> viewModel.addComment(post.id, content) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showCreatePostDialog) {
        var postContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            title = { Text("Create Post") },
            text = {
                OutlinedTextField(
                    value = postContent,
                    onValueChange = { postContent = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (postContent.isNotBlank()) {
                            viewModel.createPost(postContent)
                            showCreatePostDialog = false
                        }
                    }
                ) {
                    Text("Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PostItem(post: Post, comments: List<Comment>, onAddComment: (String) -> Unit) {
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = post.userName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = post.content, modifier = Modifier.padding(vertical = 8.dp))

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(text = "Comments (${comments.size})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

            comments.forEach { comment ->
                Column(modifier = Modifier.padding(top = 8.dp, start = 8.dp)) {
                    Text(text = comment.userName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (showCommentInput) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    trailingIcon = {
                        TextButton(onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                                showCommentInput = false
                            }
                        }) {
                            Text("Send")
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
