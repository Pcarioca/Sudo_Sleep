package com.example.sudosleep.data.auth

import com.example.sudosleep.data.preferences.UserPreferencesManager
import com.example.sudosleep.data.remote.ApiErrorParser
import com.example.sudosleep.data.remote.AuthApi
import com.example.sudosleep.data.remote.dto.AuthResponseDto
import com.example.sudosleep.data.remote.dto.FacebookAuthRequestDto
import com.example.sudosleep.data.remote.dto.ForgotPasswordRequestDto
import com.example.sudosleep.data.remote.dto.GoogleAuthRequestDto
import com.example.sudosleep.data.remote.dto.LoginRequestDto
import com.example.sudosleep.data.remote.dto.RegisterRequestDto
import com.example.sudosleep.data.remote.dto.ResetPasswordRequestDto
import com.example.sudosleep.data.remote.dto.UserDto
import com.example.sudosleep.data.remote.dto.UserProfileUpdateRequestDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Backend-connected Authentication & User Profile manager backed by Spring Boot JWT APIs
 * and hardware-encrypted token storage.
 */
class AuthManager(
    private val authApi: AuthApi,
    private val tokenStore: SessionTokenStore,
    private val preferencesManager: UserPreferencesManager,
    private val onClearLocalUserData: suspend () -> Unit = {},
    private val onClearSocialSessions: suspend () -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    data class AuthResult(
        val success: Boolean,
        val displayName: String = "",
        val email: String = "",
        val authProvider: String = "",
        val errorMessage: String? = null
    )

    data class ActionResult(
        val success: Boolean,
        val message: String? = null,
        val errorMessage: String? = null
    )

    private val _sessionExpiredMessage = MutableStateFlow<String?>(null)
    val sessionExpiredMessage: StateFlow<String?> = _sessionExpiredMessage.asStateFlow()

    init {
        // Guard against stale logged-in flags (e.g., upgrading from the old local stub without JWT tokens)
        if (preferencesManager.state.value.isLoggedIn && !tokenStore.hasSession()) {
            preferencesManager.logOut()
        }
    }

    fun hasStoredSession(): Boolean = tokenStore.hasSession()

    fun getActiveSemesterId(): Long? = tokenStore.getActiveSemesterId()

    fun consumeSessionExpiredMessage() {
        _sessionExpiredMessage.value = null
    }

    /**
     * Called by OkHttp's TokenRefreshAuthenticator when the refresh token is no longer valid.
     */
    suspend fun handleSessionExpired() {
        clearSessionAndLocalData()
        _sessionExpiredMessage.value = "Your session has expired. Please log in again."
    }

    /**
     * Validates an existing stored JWT session against GET /api/users/me on app launch.
     * Transparently refreshes expired access tokens via ApiClient's OkHttp Authenticator.
     */
    suspend fun restoreSessionIfAvailable(): Boolean {
        if (!tokenStore.hasSession()) {
            if (preferencesManager.state.value.isLoggedIn) {
                preferencesManager.logOut()
            }
            return false
        }
        return withContext(ioDispatcher) {
            try {
                val user = authApi.getMe()
                applyProfileToPreferences(user, markLoggedIn = true)
                true
            } catch (ex: Throwable) {
                if (ApiErrorParser.isUnauthorized(ex)) {
                    clearSessionAndLocalData()
                    _sessionExpiredMessage.value = "Your session has expired. Please log in again."
                    false
                } else {
                    // Offline / transient network issue: keep cached session usable until network returns
                    preferencesManager.state.value.isLoggedIn
                }
            }
        }
    }

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): AuthResult {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.length < 2) {
            return AuthResult(success = false, errorMessage = "Please enter your full name (at least 2 characters).")
        }
        if (trimmedName.length > 100) {
            return AuthResult(success = false, errorMessage = "Full name must be 100 characters or fewer.")
        }
        if (!isValidEmail(trimmedEmail)) {
            return AuthResult(success = false, errorMessage = "Please enter a valid email address.")
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return AuthResult(
                success = false,
                errorMessage = "Password must be at least $MIN_PASSWORD_LENGTH characters long."
            )
        }
        if (password != confirmPassword) {
            return AuthResult(success = false, errorMessage = "Passwords do not match.")
        }

        return withContext(ioDispatcher) {
            try {
                val response = authApi.register(
                    RegisterRequestDto(
                        displayName = trimmedName,
                        email = trimmedEmail,
                        password = password
                    )
                )
                persistAuthResponse(response)
                _sessionExpiredMessage.value = null
                AuthResult(
                    success = true,
                    displayName = response.user.displayName,
                    email = response.user.email,
                    authProvider = response.user.authProvider
                )
            } catch (ex: Throwable) {
                AuthResult(success = false, errorMessage = ApiErrorParser.parse(ex))
            }
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim()
        if (!isValidEmail(trimmedEmail)) {
            return AuthResult(success = false, errorMessage = "Please enter a valid email address.")
        }
        if (password.isBlank()) {
            return AuthResult(success = false, errorMessage = "Please enter your password.")
        }

        return withContext(ioDispatcher) {
            try {
                val response = authApi.login(LoginRequestDto(email = trimmedEmail, password = password))
                persistAuthResponse(response)
                _sessionExpiredMessage.value = null
                AuthResult(
                    success = true,
                    displayName = response.user.displayName,
                    email = response.user.email,
                    authProvider = response.user.authProvider
                )
            } catch (ex: Throwable) {
                AuthResult(success = false, errorMessage = ApiErrorParser.parse(ex))
            }
        }
    }

    suspend fun loginWithGoogleIdToken(idToken: String): AuthResult {
        if (idToken.isBlank()) {
            return AuthResult(success = false, errorMessage = "Google sign-in did not return an ID token.")
        }
        return withContext(ioDispatcher) {
            try {
                val response = authApi.loginWithGoogle(GoogleAuthRequestDto(idToken = idToken))
                persistAuthResponse(response)
                _sessionExpiredMessage.value = null
                AuthResult(
                    success = true,
                    displayName = response.user.displayName,
                    email = response.user.email,
                    authProvider = response.user.authProvider
                )
            } catch (ex: Throwable) {
                AuthResult(success = false, errorMessage = ApiErrorParser.parse(ex))
            }
        }
    }

    suspend fun loginWithFacebookAccessToken(accessToken: String): AuthResult {
        if (accessToken.isBlank()) {
            return AuthResult(success = false, errorMessage = "Facebook login did not return an access token.")
        }
        return withContext(ioDispatcher) {
            try {
                val response = authApi.loginWithFacebook(FacebookAuthRequestDto(accessToken = accessToken))
                persistAuthResponse(response)
                _sessionExpiredMessage.value = null
                AuthResult(
                    success = true,
                    displayName = response.user.displayName,
                    email = response.user.email,
                    authProvider = response.user.authProvider
                )
            } catch (ex: Throwable) {
                AuthResult(success = false, errorMessage = ApiErrorParser.parse(ex))
            }
        }
    }

    suspend fun requestPasswordReset(email: String): ActionResult {
        val trimmedEmail = email.trim()
        if (!isValidEmail(trimmedEmail)) {
            return ActionResult(success = false, errorMessage = "Please enter a valid email address.")
        }
        return withContext(ioDispatcher) {
            try {
                val response = authApi.forgotPassword(ForgotPasswordRequestDto(email = trimmedEmail))
                ActionResult(success = true, message = response.message)
            } catch (ex: Throwable) {
                ActionResult(success = false, errorMessage = ApiErrorParser.parse(ex))
            }
        }
    }

    suspend fun confirmPasswordReset(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ): ActionResult {
        val trimmedEmail = email.trim()
        val trimmedCode = code.trim()
        if (!isValidEmail(trimmedEmail)) {
            return ActionResult(success = false, errorMessage = "Please enter a valid email address.")
        }
        if (trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
            return ActionResult(success = false, errorMessage = "Please enter the 6-digit reset code sent to your email.")
        }
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            return ActionResult(
                success = false,
                errorMessage = "New password must be at least $MIN_PASSWORD_LENGTH characters long."
            )
        }
        if (newPassword != confirmPassword) {
            return ActionResult(success = false, errorMessage = "Passwords do not match.")
        }

        return withContext(ioDispatcher) {
            try {
                val response = authApi.resetPassword(
                    ResetPasswordRequestDto(
                        email = trimmedEmail,
                        code = trimmedCode,
                        newPassword = newPassword
                    )
                )
                ActionResult(success = true, message = response.message)
            } catch (ex: Throwable) {
                ActionResult(success = false, errorMessage = ApiErrorParser.parse(ex))
            }
        }
    }

    suspend fun updateDisplayName(newDisplayName: String): AuthResult {
        val trimmed = newDisplayName.trim()
        if (trimmed.length < 2) {
            return AuthResult(success = false, errorMessage = "Display name must be at least 2 characters.")
        }
        if (trimmed.length > 100) {
            return AuthResult(success = false, errorMessage = "Display name must be 100 characters or fewer.")
        }
        return withContext(ioDispatcher) {
            try {
                val updated = authApi.updateMe(UserProfileUpdateRequestDto(displayName = trimmed))
                applyProfileToPreferences(updated, markLoggedIn = false)
                AuthResult(
                    success = true,
                    displayName = updated.displayName,
                    email = updated.email
                )
            } catch (ex: Throwable) {
                AuthResult(success = false, errorMessage = ApiErrorParser.parse(ex))
            }
        }
    }

    /**
     * Best-effort background sync of onboarding completion or default reminder offsets to /api/users/me.
     */
    suspend fun syncProfilePreferences(
        hasCompletedOnboarding: Boolean? = null,
        defaultReminderOffsetsCsv: String? = null
    ) {
        if (!tokenStore.hasSession()) return
        val currentName = preferencesManager.state.value.userDisplayName.takeIf { it.isNotBlank() } ?: return
        withContext(ioDispatcher) {
            runCatching {
                val updated = authApi.updateMe(
                    UserProfileUpdateRequestDto(
                        displayName = currentName,
                        hasCompletedOnboarding = hasCompletedOnboarding,
                        defaultReminderOffsetsCsv = defaultReminderOffsetsCsv
                    )
                )
                applyProfileToPreferences(updated, markLoggedIn = false)
            }
        }
    }

    suspend fun logout() {
        _sessionExpiredMessage.value = null
        clearSessionAndLocalData()
    }

    private suspend fun clearSessionAndLocalData() {
        withContext(ioDispatcher) {
            runCatching { onClearSocialSessions() }
            runCatching { onClearLocalUserData() }
            tokenStore.clear()
            preferencesManager.logOut()
        }
    }

    private fun persistAuthResponse(response: AuthResponseDto) {
        tokenStore.saveSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.user.id,
            activeSemesterId = response.activeSemesterId
        )
        applyProfileToPreferences(response.user, markLoggedIn = true)
    }

    private fun applyProfileToPreferences(user: UserDto, markLoggedIn: Boolean) {
        if (markLoggedIn) {
            preferencesManager.setLoggedIn(
                displayName = user.displayName,
                email = user.email,
                provider = user.authProvider,
                hasCompletedOnboarding = user.hasCompletedOnboarding,
                defaultReminderOffsetsCsv = user.defaultReminderOffsetsCsv
            )
        } else {
            preferencesManager.updateUserProfile(
                displayName = user.displayName,
                email = user.email,
                provider = user.authProvider,
                hasCompletedOnboarding = user.hasCompletedOnboarding,
                defaultReminderOffsetsCsv = user.defaultReminderOffsetsCsv
            )
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        internal fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)
    }
}
