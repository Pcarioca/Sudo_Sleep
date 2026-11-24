package com.example.sudosleep

import com.example.sudosleep.data.auth.AuthManager
import com.example.sudosleep.data.remote.ApiErrorParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException

class AuthErrorAndValidationTest {

    @Test
    fun emailValidationAcceptsStandardEmailsAndRejectsInvalidOnes() {
        assertTrue(AuthManager.isValidEmail("alex.rivera@university.edu"))
        assertTrue(AuthManager.isValidEmail("student+1@uni.ro"))
        assertFalse(AuthManager.isValidEmail(""))
        assertFalse(AuthManager.isValidEmail("justusername"))
        assertFalse(AuthManager.isValidEmail("missing@domain"))
        assertFalse(AuthManager.isValidEmail("spaces in@email.com"))
    }

    @Test
    fun apiErrorParserExtractsRfc9457DetailAndFieldErrors() {
        val problemJson = """
            {"type":"about:blank","title":"Unauthorized","status":401,"detail":"Invalid email or password"}
        """.trimIndent()
        val http401 = HttpException(
            Response.error<Any>(401, problemJson.toResponseBody("application/problem+json".toMediaType()))
        )
        assertEquals("Invalid email or password", ApiErrorParser.parse(http401))
        assertTrue(ApiErrorParser.isUnauthorized(http401))

        val validationJson = """
            {"type":"about:blank","title":"Bad Request","status":400,"errors":{"password":"size must be between 8 and 128"}}
        """.trimIndent()
        val http400 = HttpException(
            Response.error<Any>(400, validationJson.toResponseBody("application/problem+json".toMediaType()))
        )
        assertEquals("Password: size must be between 8 and 128", ApiErrorParser.parse(http400))
        assertFalse(ApiErrorParser.isUnauthorized(http400))
    }

    @Test
    fun apiErrorParserFormatsNetworkConnectionErrorsClearly() {
        val message = ApiErrorParser.parse(ConnectException("Failed to connect to /10.0.2.2:8080"))
        assertTrue(message.contains("Cannot reach the Sudo Sleep server"))
    }
}
