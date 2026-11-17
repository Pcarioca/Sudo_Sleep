package com.example.sudosleep.data.auth

import com.example.sudosleep.data.preferences.UserPreferencesManager

/**
 * Decoupled frontend Authentication Manager stub.
 * Currently accepts any non-empty credentials or Google Quick Sign-In and persists session state locally.
 * Ready to be wired to Firebase Auth or a custom OAuth2 backend later without changing UI code.
 */
class AuthManager(private val preferencesManager: UserPreferencesManager) {

    data class AuthResult(
        val success: Boolean,
        val displayName: String,
        val email: String,
        val errorMessage: String? = null
    )

    fun loginWithEmailOrUsername(identifier: String, password: String): AuthResult {
        val trimmedId = identifier.trim()
        if (trimmedId.isEmpty()) {
            return AuthResult(
                success = false,
                displayName = "",
                email = "",
                errorMessage = "Please enter your email or username ✨"
            )
        }
        if (password.isBlank()) {
            return AuthResult(
                success = false,
                displayName = "",
                email = "",
                errorMessage = "Please enter a password 🔑"
            )
        }

        val displayName = if (trimmedId.contains("@")) {
            trimmedId.substringBefore("@")
                .replace(".", " ")
                .replace("_", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { part ->
                    part.replaceFirstChar { it.uppercase() }
                }
                .ifBlank { "Sudo Scholar" }
        } else {
            trimmedId.replaceFirstChar { it.uppercase() }
        }

        val email = if (trimmedId.contains("@")) trimmedId else "$trimmedId@sudosleep.edu"

        preferencesManager.setLoggedIn(
            displayName = displayName,
            email = email,
            provider = "Email"
        )
        return AuthResult(success = true, displayName = displayName, email = email)
    }

    fun continueWithGoogle(mockAccountName: String = "Alex Rivera", mockEmail: String = "alex.rivera@gmail.com"): AuthResult {
        preferencesManager.setLoggedIn(
            displayName = mockAccountName,
            email = mockEmail,
            provider = "Google"
        )
        return AuthResult(success = true, displayName = mockAccountName, email = mockEmail)
    }

    fun logout() {
        preferencesManager.logOut()
    }
}
