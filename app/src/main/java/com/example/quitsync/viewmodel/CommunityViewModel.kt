package com.example.quitsync.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.quitsync.model.Comment
import com.example.quitsync.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

sealed class CommunityUiState {
    object Idle : CommunityUiState()
    object Loading : CommunityUiState()
    data class Error(val message: String) : CommunityUiState()
}

class CommunityViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _uiState = mutableStateOf<CommunityUiState>(CommunityUiState.Idle)
    val uiState: State<CommunityUiState> = _uiState

    private val _posts = mutableStateOf<List<Post>>(emptyList())
    val posts: State<List<Post>> = _posts

    private val _commentsMap = mutableStateOf<Map<String, List<Comment>>>(emptyMap())
    val commentsMap: State<Map<String, List<Comment>>> = _commentsMap

    init {
        fetchPosts()
    }

    fun fetchPosts() {
        _uiState.value = CommunityUiState.Loading
        db.collection("forum_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CommunityViewModel", "Firestore Error: ${e.message}")
                    _uiState.value = CommunityUiState.Error("Failed to load posts.")
                    fetchWithoutOrdering()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val postList = snapshot.toObjects(Post::class.java)
                    _posts.value = postList
                    _uiState.value = CommunityUiState.Idle

                    postList.forEach { post ->
                        fetchCommentsForPost(post.id)
                    }
                }
            }
    }

    private fun fetchWithoutOrdering() {
        db.collection("forum_posts").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val postList = snapshot.toObjects(Post::class.java)
                    .sortedByDescending { it.timestamp }
                _posts.value = postList
            }
        }
    }

    fun createPost(title: String, description: String) {
        val user = auth.currentUser ?: return
        val docRef = db.collection("forum_posts").document()
        val post = Post(
            id = docRef.id,
            userId = user.uid,
            userName = user.email?.substringBefore("@") ?: "User",
            title = title,
            description = description
        )

        docRef.set(post)
            .addOnFailureListener { e ->
                _uiState.value = CommunityUiState.Error("Post failed: ${e.localizedMessage}")
            }
    }

    fun toggleLike(post: Post) {
        val userId = currentUserId ?: return
        val postRef = db.collection("forum_posts").document(post.id)

        val update = if (post.likedBy.contains(userId)) {
            FieldValue.arrayRemove(userId)
        } else {
            FieldValue.arrayUnion(userId)
        }

        postRef.update("likedBy", update)
            .addOnFailureListener { e ->
                Log.e("CommunityViewModel", "Like failed: ${e.message}")
                _uiState.value = CommunityUiState.Error("Could not update like. Check rules.")
            }
    }

    fun addComment(postId: String, content: String) {
        val user = auth.currentUser ?: return
        val docRef = db.collection("forum_posts").document(postId).collection("comments").document()
        val comment = Comment(
            id = docRef.id,
            postId = postId,
            userId = user.uid,
            userName = user.email?.substringBefore("@") ?: "User",
            content = content
        )

        docRef.set(comment)
            .addOnFailureListener { e ->
                Log.e("CommunityViewModel", "Comment failed: ${e.message}")
                _uiState.value = CommunityUiState.Error("Could not add comment. Check rules.")
            }
    }

    private fun fetchCommentsForPost(postId: String) {
        db.collection("forum_posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                val currentMap = _commentsMap.value.toMutableMap()
                currentMap[postId] = comments
                _commentsMap.value = currentMap
            }
    }

    fun updatePost(postId: String, newTitle: String, newDescription: String) {
        if (postId.isEmpty()) return
        db.collection("forum_posts").document(postId)
            .update(mapOf("title" to newTitle, "description" to newDescription))
            .addOnFailureListener { e ->
                _uiState.value = CommunityUiState.Error("Update failed: ${e.localizedMessage}")
            }
    }

    fun deletePost(postId: String) {
        if (postId.isEmpty()) return
        db.collection("forum_posts").document(postId).delete()
    }

    fun resetState() { _uiState.value = CommunityUiState.Idle }
}
