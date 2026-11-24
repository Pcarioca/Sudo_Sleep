package com.example.sudosleep.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    val displayName: String,
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequestDto(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class GoogleAuthRequestDto(
    val idToken: String
)

@JsonClass(generateAdapter = true)
data class FacebookAuthRequestDto(
    val accessToken: String
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequestDto(
    val email: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequestDto(
    val email: String,
    val code: String,
    val newPassword: String
)

@JsonClass(generateAdapter = true)
data class UserProfileUpdateRequestDto(
    val displayName: String,
    val email: String? = null,
    val hasCompletedOnboarding: Boolean? = null,
    val defaultReminderOffsetsCsv: String? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: Long,
    val email: String,
    val displayName: String,
    val authProvider: String,
    val hasCompletedOnboarding: Boolean,
    val defaultReminderOffsetsCsv: String,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresInSeconds: Long,
    val user: UserDto,
    val activeSemesterId: Long? = null
)

@JsonClass(generateAdapter = true)
data class MessageResponseDto(
    val message: String
)

@JsonClass(generateAdapter = true)
data class ProblemDetailDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val errors: Map<String, String>? = null
)
