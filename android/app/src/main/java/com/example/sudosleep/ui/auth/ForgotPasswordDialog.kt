package com.example.sudosleep.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sudosleep.data.auth.AuthManager
import kotlinx.coroutines.launch

private enum class ResetStep {
    REQUEST_CODE,
    CONFIRM_RESET
}

@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    authManager: AuthManager,
    onDismiss: () -> Unit,
    onPasswordResetSuccess: (email: String, message: String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(ResetStep.REQUEST_CODE) }
    var email by remember { mutableStateOf(initialEmail) }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (step == ResetStep.REQUEST_CODE) "Reset Password" else "Enter Reset Code",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(visible = infoMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = infoMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                AnimatedContent(targetState = step, label = "reset_password_step") { activeStep ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (activeStep == ResetStep.REQUEST_CODE) {
                            Text(
                                text = "Enter your account email and we'll send you a 6-digit password reset code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                label = { Text("Email") },
                                placeholder = { Text("student@university.edu") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                enabled = !isLoading,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { input ->
                                    if (input.length <= 6 && input.all { it.isDigit() }) {
                                        code = input
                                        errorMessage = null
                                    }
                                },
                                label = { Text("6-Digit Reset Code") },
                                placeholder = { Text("123456") },
                                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                                enabled = !isLoading,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = {
                                    newPassword = it
                                    errorMessage = null
                                },
                                label = { Text("New Password") },
                                supportingText = { Text("At least 8 characters") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                enabled = !isLoading,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    errorMessage = null
                                },
                                label = { Text("Confirm New Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                enabled = !isLoading,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            TextButton(
                                onClick = {
                                    if (!isLoading) {
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            val result = authManager.requestPasswordReset(email)
                                            isLoading = false
                                            if (result.success) {
                                                infoMessage = result.message ?: "A fresh reset code has been sent."
                                            } else {
                                                errorMessage = result.errorMessage
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading
                            ) {
                                Text("Didn't receive a code? Resend")
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (step == ResetStep.CONFIRM_RESET) {
                        step = ResetStep.REQUEST_CODE
                        errorMessage = null
                    } else {
                        onDismiss()
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (step == ResetStep.CONFIRM_RESET) "Back" else "Cancel")
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        if (step == ResetStep.REQUEST_CODE) {
                            val result = authManager.requestPasswordReset(email)
                            isLoading = false
                            if (result.success) {
                                infoMessage = result.message
                                step = ResetStep.CONFIRM_RESET
                            } else {
                                errorMessage = result.errorMessage
                            }
                        } else {
                            val result = authManager.confirmPasswordReset(
                                email = email,
                                code = code,
                                newPassword = newPassword,
                                confirmPassword = confirmPassword
                            )
                            isLoading = false
                            if (result.success) {
                                onPasswordResetSuccess(
                                    email.trim(),
                                    result.message ?: "Password reset successfully. You can now log in."
                                )
                            } else {
                                errorMessage = result.errorMessage
                            }
                        }
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (step == ResetStep.REQUEST_CODE) "Send Code" else "Reset Password",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
