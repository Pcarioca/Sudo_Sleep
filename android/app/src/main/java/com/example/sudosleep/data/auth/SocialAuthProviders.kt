package com.example.sudosleep.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.sudosleep.BuildConfig
import com.example.sudosleep.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import kotlin.coroutines.resume

sealed class SocialTokenResult {
    data class Success(val token: String) : SocialTokenResult()
    data object Cancelled : SocialTokenResult()
    data class Error(val message: String) : SocialTokenResult()
}

/**
 * Pure configuration validation & nonce helpers for Google and Facebook sign-in.
 */
object SocialAuthConfig {

    private val secureRandom = SecureRandom()

    fun isConfiguredValue(value: String?): Boolean {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return false
        val upper = trimmed.uppercase(Locale.ROOT)
        return !upper.contains("YOUR_") &&
            !upper.contains("YOUR-") &&
            !upper.contains("REPLACE_") &&
            !upper.contains("PLACEHOLDER") &&
            !upper.contains("TODO") &&
            !trimmed.startsWith("<") &&
            !trimmed.endsWith(">")
    }

    /**
     * Prefers an explicitly configured BuildConfig value (from android/local.properties),
     * falling back to R.string.default_web_client_id (in res/values/strings.xml).
     */
    fun resolveGoogleWebClientId(buildConfigValue: String?, resourceValue: String?): String? {
        if (isConfiguredValue(buildConfigValue)) {
            return buildConfigValue!!.trim()
        }
        if (isConfiguredValue(resourceValue)) {
            return resourceValue!!.trim()
        }
        return null
    }

    fun isFacebookConfigured(
        appId: String?,
        clientToken: String?,
        protocolScheme: String?
    ): Boolean {
        if (!isConfiguredValue(appId) || !isConfiguredValue(clientToken) || !isConfiguredValue(protocolScheme)) {
            return false
        }
        val trimmedId = appId!!.trim()
        val trimmedScheme = protocolScheme!!.trim()
        return trimmedScheme == "fb$trimmedId"
    }

    fun generateNonce(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength.coerceAtLeast(16))
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

/**
 * Modern Android Credential Manager implementation for Sign in with Google
 * (replaces deprecated GoogleSignInClient).
 */
class GoogleCredentialAuthHelper(context: Context) {

    private val credentialManager: CredentialManager =
        CredentialManager.create(context.applicationContext)

    suspend fun requestGoogleIdToken(activity: Activity): SocialTokenResult {
        val resourceClientId = runCatching { activity.getString(R.string.default_web_client_id) }.getOrNull()
        val webClientId = SocialAuthConfig.resolveGoogleWebClientId(
            buildConfigValue = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            resourceValue = resourceClientId
        ) ?: return SocialTokenResult.Error(
            "Google Sign-In is not configured yet. Set default_web_client_id in strings.xml " +
                "(or GOOGLE_WEB_CLIENT_ID in local.properties)."
        )

        // Step 1: Try previously authorized Google accounts on the device first.
        val authorizedResult = requestCredential(
            activity = activity,
            webClientId = webClientId,
            filterByAuthorizedAccounts = true
        )
        if (authorizedResult != null) {
            return authorizedResult
        }

        // Step 2: No previously authorized account found -> allow selecting any Google account.
        return requestCredential(
            activity = activity,
            webClientId = webClientId,
            filterByAuthorizedAccounts = false
        ) ?: SocialTokenResult.Error(
            "No Google account is available on this device. Add a Google account in Android Settings and try again."
        )
    }

    /**
     * Returns null only when [NoCredentialException] is thrown so the caller can fall back
     * from authorized-only accounts to all Google accounts on the device.
     */
    private suspend fun requestCredential(
        activity: Activity,
        webClientId: String,
        filterByAuthorizedAccounts: Boolean
    ): SocialTokenResult? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .setNonce(SocialAuthConfig.generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val activityWrapper = MutableContextWrapper(activity)
        return try {
            val response = credentialManager.getCredential(
                request = request,
                context = activityWrapper
            )
            extractGoogleIdToken(response)
        } catch (_: GetCredentialCancellationException) {
            SocialTokenResult.Cancelled
        } catch (_: NoCredentialException) {
            null
        } catch (_: GetCredentialProviderConfigurationException) {
            SocialTokenResult.Error(
                "Google Sign-In requires up-to-date Google Play Services on this device."
            )
        } catch (_: GetCredentialInterruptedException) {
            SocialTokenResult.Error("Google Sign-In was interrupted. Please try again.")
        } catch (ex: GetCredentialException) {
            val detail = ex.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                ?: "Google Sign-In could not be completed. Check your internet connection and OAuth SHA-1 setup."
            SocialTokenResult.Error(detail)
        } catch (ex: Throwable) {
            SocialTokenResult.Error(
                ex.message?.takeIf { it.isNotBlank() } ?: "Google Sign-In failed unexpectedly."
            )
        }
    }

    private fun extractGoogleIdToken(response: GetCredentialResponse): SocialTokenResult {
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return try {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleCredential.idToken
                if (idToken.isBlank()) {
                    SocialTokenResult.Error("Google did not return a valid ID token.")
                } else {
                    SocialTokenResult.Success(idToken)
                }
            } catch (_: GoogleIdTokenParsingException) {
                SocialTokenResult.Error("Could not parse Google Sign-In credentials. Please try again.")
            }
        }
        return SocialTokenResult.Error("Unsupported credential type returned by Google Sign-In.")
    }

    /**
     * Clears any cached Credential Manager session state on logout so the user
     * can freely pick a different Google account next time.
     */
    suspend fun clearCredentialState() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

/**
 * Official Facebook Login SDK wrapper using Jetpack ActivityResultRegistryOwner
 * (no deprecated onActivityResult overrides required).
 */
class FacebookLoginAuthHelper {

    suspend fun requestFacebookAccessToken(activity: ComponentActivity): SocialTokenResult {
        val appId = runCatching { activity.getString(R.string.facebook_app_id) }.getOrNull()
        val clientToken = runCatching { activity.getString(R.string.facebook_client_token) }.getOrNull()
        val protocolScheme = runCatching { activity.getString(R.string.fb_login_protocol_scheme) }.getOrNull()

        if (!SocialAuthConfig.isFacebookConfigured(appId, clientToken, protocolScheme)) {
            return SocialTokenResult.Error(
                "Facebook Login is not configured yet. Set facebook_app_id, facebook_client_token, " +
                    "and fb_login_protocol_scheme in strings.xml."
            )
        }

        return withContext(Dispatchers.Main.immediate) {
            runCatching {
                if (!FacebookSdk.isInitialized()) {
                    FacebookSdk.fullyInitialize()
                }
            }

            suspendCancellableCoroutine { continuation ->
                val callbackManager = CallbackManager.Factory.create()
                val loginManager = LoginManager.getInstance()

                val callback = object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        runCatching { loginManager.unregisterCallback(callbackManager) }
                        if (!continuation.isActive) return
                        val token = result.accessToken.token
                        if (token.isBlank()) {
                            continuation.resume(
                                SocialTokenResult.Error("Facebook login did not return a valid access token.")
                            )
                        } else if (result.recentlyDeniedPermissions.contains("email")) {
                            continuation.resume(
                                SocialTokenResult.Error(
                                    "Email permission was declined. Please allow email access on Facebook to sign in."
                                )
                            )
                        } else {
                            continuation.resume(SocialTokenResult.Success(token))
                        }
                    }

                    override fun onCancel() {
                        runCatching { loginManager.unregisterCallback(callbackManager) }
                        if (continuation.isActive) {
                            continuation.resume(SocialTokenResult.Cancelled)
                        }
                    }

                    override fun onError(error: FacebookException) {
                        runCatching { loginManager.unregisterCallback(callbackManager) }
                        if (!continuation.isActive) return
                        val raw = error.message.orEmpty()
                        val friendly = when {
                            raw.contains("CONNECTION_FAILURE", ignoreCase = true) ||
                                raw.contains("network", ignoreCase = true) ||
                                raw.contains("Unable to resolve host", ignoreCase = true) ->
                                "Could not reach Facebook. Please check your internet connection."
                            raw.isNotBlank() -> raw
                            else -> "Facebook Login failed. Please try again."
                        }
                        continuation.resume(SocialTokenResult.Error(friendly))
                    }
                }

                continuation.invokeOnCancellation {
                    runCatching { loginManager.unregisterCallback(callbackManager) }
                }

                try {
                    loginManager.registerCallback(callbackManager, callback)
                    loginManager.logInWithReadPermissions(
                        activity,
                        callbackManager,
                        listOf("email", "public_profile")
                    )
                } catch (ex: Throwable) {
                    runCatching { loginManager.unregisterCallback(callbackManager) }
                    if (continuation.isActive) {
                        continuation.resume(
                            SocialTokenResult.Error(
                                ex.message?.takeIf { it.isNotBlank() }
                                    ?: "Unable to launch Facebook Login on this device."
                            )
                        )
                    }
                }
            }
        }
    }

    fun logOut() {
        runCatching {
            if (FacebookSdk.isInitialized()) {
                LoginManager.getInstance().logOut()
            }
        }
    }
}

tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
