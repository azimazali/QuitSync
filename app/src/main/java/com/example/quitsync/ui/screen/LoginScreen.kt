package com.example.quitsync.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("")}
    var password by remember { mutableStateOf("")}
    var errorMessage by remember { mutableStateOf<String?>(null)}
    var showResetDialog by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text(text = "Welcome Back", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(text = "Sign in to continue your journey", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = {Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange =  { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { showResetDialog = true }) {
                Text("Forgot Password?")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                errorMessage = null
                if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.signIn(email, password) { success, error ->
                        if (success) onLoginSuccess() else errorMessage = error
                    }
                } else {
                    errorMessage = "Please enter your credentials"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ){
            Text("Login")
        }

        TextButton(onClick = onNavigateToSignUp) {
            Text("Don't have an account? Sign Up")
        }

        if (resetMessage != null) {
            Text(text = resetMessage!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
        }
    }

    if (showResetDialog) {
        ForgotPasswordDialog(
            onDismiss = { showResetDialog = false },
            onReset = { resetEmail ->
                viewModel.resetPassword(resetEmail) { success, message ->
                    resetMessage = message
                    showResetDialog = false
                }
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit, onReset: (String) -> Unit) {
    var email by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(text = "Reset Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Enter your email address and we'll send you a link to reset your password.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onReset(email) },
                        enabled = email.isNotBlank()
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}
