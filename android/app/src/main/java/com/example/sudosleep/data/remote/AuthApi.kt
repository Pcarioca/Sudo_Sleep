package com.example.sudosleep.data.remote

import com.example.sudosleep.data.remote.dto.AuthResponseDto
import com.example.sudosleep.data.remote.dto.FacebookAuthRequestDto
import com.example.sudosleep.data.remote.dto.ForgotPasswordRequestDto
import com.example.sudosleep.data.remote.dto.GoogleAuthRequestDto
import com.example.sudosleep.data.remote.dto.LoginRequestDto
import com.example.sudosleep.data.remote.dto.MessageResponseDto
import com.example.sudosleep.data.remote.dto.RefreshTokenRequestDto
import com.example.sudosleep.data.remote.dto.RegisterRequestDto
import com.example.sudosleep.data.remote.dto.ResetPasswordRequestDto
import com.example.sudosleep.data.remote.dto.UserDto
import com.example.sudosleep.data.remote.dto.UserProfileUpdateRequestDto
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): AuthResponseDto

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequestDto): AuthResponseDto

    @POST("api/auth/refresh")
    fun refreshBlocking(@Body request: RefreshTokenRequestDto): Call<AuthResponseDto>

    @POST("api/auth/oauth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequestDto): AuthResponseDto

    @POST("api/auth/oauth/facebook")
    suspend fun loginWithFacebook(@Body request: FacebookAuthRequestDto): AuthResponseDto

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto): MessageResponseDto

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto): MessageResponseDto

    @GET("api/users/me")
    suspend fun getMe(): UserDto

    @PUT("api/users/me")
    suspend fun updateMe(@Body request: UserProfileUpdateRequestDto): UserDto

    @DELETE("api/users/me")
    suspend fun deleteMe(): Response<Unit>
}
