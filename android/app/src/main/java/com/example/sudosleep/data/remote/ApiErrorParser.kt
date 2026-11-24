package com.example.sudosleep.data.remote

import com.example.sudosleep.data.remote.dto.ProblemDetailDto
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ApiErrorParser {

    private val moshi: Moshi = Moshi.Builder().build()
    private val problemAdapter = moshi.adapter(ProblemDetailDto::class.java)

    fun parse(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> parseHttpException(throwable)
            is ConnectException, is UnknownHostException ->
                "Cannot reach the Sudo Sleep server. Please check that the backend is running and your network connection is active."
            is SocketTimeoutException ->
                "The server took too long to respond. Please try again in a moment."
            is IOException ->
                "Network error while contacting the server. Please check your connection."
            else -> throwable.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
        }
    }

    fun isUnauthorized(throwable: Throwable): Boolean {
        return throwable is HttpException && throwable.code() == 401
    }

    private fun parseHttpException(ex: HttpException): String {
        val rawBody = runCatching { ex.response()?.errorBody()?.string() }.getOrNull()
        val problem = rawBody?.takeIf { it.isNotBlank() }?.let { json ->
            runCatching { problemAdapter.fromJson(json) }.getOrNull()
        }

        val fieldErrors = problem?.errors
        if (!fieldErrors.isNullOrEmpty()) {
            return fieldErrors.entries.joinToString("\n") { (field, msg) ->
                "${formatFieldName(field)}: $msg"
            }
        }
        val detail = problem?.detail?.trim()
        if (!detail.isNullOrEmpty()) {
            return detail
        }
        val title = problem?.title?.trim()
        if (!title.isNullOrEmpty() && !title.equals("Bad Request", ignoreCase = true)) {
            return title
        }

        return when (ex.code()) {
            400 -> "Please check your input and try again."
            401 -> "Invalid credentials or expired session. Please log in again."
            403 -> "You do not have permission to perform this action."
            404 -> "Requested resource was not found."
            409 -> "This email is already registered."
            in 500..599 -> "Server error (${ex.code()}). Please try again shortly."
            else -> "Request failed (HTTP ${ex.code()})."
        }
    }

    private fun formatFieldName(field: String): String {
        return when (field) {
            "displayName" -> "Full Name"
            "newPassword" -> "New Password"
            "email" -> "Email"
            "password" -> "Password"
            "code" -> "Reset Code"
            else -> field.replaceFirstChar { it.uppercase() }
        }
    }
}
