package com.example.quitsync.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.viewmodel.AdminViewModel
import com.example.quitsync.model.User

@Composable
fun AdminScreen(viewModel: AdminViewModel = viewModel()) {
    val users by viewModel.users

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Admin Panel",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(users) { user ->
                UserAdminItem(
                    user = user,
                    onDelete = { viewModel.deleteUser(user.uid) },
                    onToggleRole = {
                        val newRole = if (user.role == "admin") "user" else "admin"
                        viewModel.updateUserRole(user.uid, newRole)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun UserAdminItem(user: User, onDelete: () -> Unit, onToggleRole: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.email, fontWeight = FontWeight.Bold)
            Text(text = "Role: ${user.role}", style = MaterialTheme.typography.bodySmall)
        }

        Row {
            TextButton(onClick = onToggleRole) {
                Text(if (user.role == "admin") "Demote" else "Promote")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete User",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
