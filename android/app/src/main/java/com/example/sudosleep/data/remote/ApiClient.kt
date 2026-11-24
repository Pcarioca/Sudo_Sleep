package com.example.sudosleep.data.remote

import com.example.sudosleep.BuildConfig
import com.example.sudosleep.data.auth.SessionTokenStore
import com.example.sudosleep.data.remote.dto.RefreshTokenRequestDto
import com.squareup.moshi.Moshi
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Configures Retrofit + OkHttp for communicating with the Sudo Sleep Spring Boot API.
 * Automatically attaches Bearer JWT tokens and transparently refreshes expired access tokens on 401.
 */
class ApiClient(
    private val tokenStore: SessionTokenStore,
    baseUrl: String = BuildConfig.API_BASE_URL,
    private val onSessionExpired: () -> Unit = {}
) {
    private val normalizedBaseUrl: String = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    private val moshi: Moshi = Moshi.Builder().build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // Separate plain client for /api/auth/refresh inside OkHttp Authenticator (avoids recursion)
    private val refreshAuthApi: AuthApi by lazy {
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(refreshClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApi::class.java)
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val path = original.url.encodedPath
        val isPublicAuthRoute = path.startsWith("/api/auth/")
        if (isPublicAuthRoute || original.header("Authorization") != null) {
            return@Interceptor chain.proceed(original)
        }
        val accessToken = tokenStore.getAccessToken()
        val request = if (!accessToken.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val tokenRefreshAuthenticator = object : Authenticator {
        private val refreshLock = Any()

        override fun authenticate(route: Route?, response: Response): Request? {
            if (response.request.url.encodedPath.startsWith("/api/auth/")) {
                return null
            }
            if (responseCount(response) >= 2) {
                return null
            }

            synchronized(refreshLock) {
                val requestAuthHeader = response.request.header("Authorization")
                val latestAccessToken = tokenStore.getAccessToken()

                // If another concurrent request already refreshed the token, retry immediately with the new one
                if (!latestAccessToken.isNullOrBlank() && requestAuthHeader != "Bearer $latestAccessToken") {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $latestAccessToken")
                        .build()
                }

                val refreshToken = tokenStore.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    expireSession()
                    return null
                }

                val refreshResult = runCatching {
                    refreshAuthApi.refreshBlocking(RefreshTokenRequestDto(refreshToken)).execute()
                }.getOrNull()

                val body = if (refreshResult != null && refreshResult.isSuccessful) {
                    refreshResult.body()
                } else {
                    null
                }

                if (body == null) {
                    if (refreshResult == null || refreshResult.code() in listOf(401, 403)) {
                        expireSession()
                    }
                    return null
                }

                tokenStore.saveSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userId = body.user.id,
                    activeSemesterId = body.activeSemesterId
                )

                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${body.accessToken}")
                    .build()
            }
        }

        private fun expireSession() {
            tokenStore.clear()
            onSessionExpired()
        }

        private fun responseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(tokenRefreshAuthenticator)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val academicApi: AcademicApi by lazy {
        retrofit.create(AcademicApi::class.java)
    }
}
