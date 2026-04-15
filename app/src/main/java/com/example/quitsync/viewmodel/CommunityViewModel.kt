package com.example.quitsync.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.quitsync.model.Comment
import com.example.quitsync.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommunityViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _posts = mutableStateOf<List<Post>>(emptyList())
    val posts: State<List<Post>> = _posts

    private val _commentsMap = mutableStateOf<Map<String, List<Comment>>>(emptyMap())
    val commentsMap: State<Map<String, List<Comment>>> = _commentsMap

    init {
        fetchPosts()
    }

    fun fetchPosts() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val postList = snapshot?.toObjects(Post::class.java) ?: emptyList()
                _posts.value = postList

                // Fetch comments for each post
                postList.forEach { post ->
                    fetchCommentsForPost(post.id)
                }
            }
    }

    private fun fetchCommentsForPost(postId: String) {
        db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                val currentMap = _commentsMap.value.toMutableMap()
                currentMap[postId] = comments
                _commentsMap.value = currentMap
            }
    }

    fun createPost(content: String) {
        val user = auth.currentUser ?: return
        val docRef = db.collection("posts").document()
        val post = Post(
            id = docRef.id,
            userId = user.uid,
            userName = user.email?.substringBefore("@") ?: "User",
            content = content
        )
        docRef.set(post)
    }

    fun addComment(postId: String, content: String) {
        val user = auth.currentUser ?: return
        val docRef = db.collection("posts").document(postId).collection("comments").document()
        val comment = Comment(
            id = docRef.id,
            postId = postId,
            userId = user.uid,
            userName = user.email?.substringBefore("@") ?: "User",
            content = content
        )
        docRef.set(comment)
    }
}
