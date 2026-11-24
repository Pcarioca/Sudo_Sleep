package com.example.sudosleep.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SessionTokenStore {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getUserId(): Long?
    fun getActiveSemesterId(): Long?
    fun hasSession(): Boolean = !getAccessToken().isNullOrBlank() && !getRefreshToken().isNullOrBlank()
    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        activeSemesterId: Long?
    )
    fun updateActiveSemesterId(semesterId: Long)
    fun clear()
}

/**
 * Hardware-backed Android Keystore (AES-256-GCM) encrypted token store built on private
 * SharedPreferences. Requires API 29+ (matches minSdk = 29) and avoids deprecated crypto wrappers.
 */
class EncryptedTokenStore(context: Context) : SessionTokenStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun getAccessToken(): String? = decrypt(prefs.getString(KEY_ACCESS_TOKEN, null))

    @Synchronized
    override fun getRefreshToken(): String? = decrypt(prefs.getString(KEY_REFRESH_TOKEN, null))

    @Synchronized
    override fun getUserId(): Long? {
        val raw = prefs.getLong(KEY_USER_ID, -1L)
        return if (raw > 0L) raw else null
    }

    @Synchronized
    override fun getActiveSemesterId(): Long? {
        val raw = prefs.getLong(KEY_ACTIVE_SEMESTER_ID, -1L)
        return if (raw > 0L) raw else null
    }

    @Synchronized
    override fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        activeSemesterId: Long?
    ) {
        val encryptedAccess = encrypt(accessToken)
        val encryptedRefresh = encrypt(refreshToken)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, encryptedAccess)
            .putString(KEY_REFRESH_TOKEN, encryptedRefresh)
            .putLong(KEY_USER_ID, userId)
            .putLong(KEY_ACTIVE_SEMESTER_ID, activeSemesterId ?: -1L)
            .apply()
    }

    @Synchronized
    override fun updateActiveSemesterId(semesterId: Long) {
        prefs.edit()
            .putLong(KEY_ACTIVE_SEMESTER_ID, semesterId)
            .apply()
    }

    @Synchronized
    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val payload = ByteBuffer.allocate(4 + iv.size + cipherBytes.size)
            .putInt(iv.size)
            .put(iv)
            .put(cipherBytes)
            .array()
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return try {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(payload)
            val ivLength = buffer.int
            if (ivLength !in 12..16 || ivLength > buffer.remaining()) {
                return null
            }
            val iv = ByteArray(ivLength)
            buffer.get(iv)
            val cipherBytes = ByteArray(buffer.remaining())
            buffer.get(cipherBytes)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // If keystore key was invalidated (e.g., backup restore on another device), clear stale ciphertext
            clear()
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "sudo_sleep_secure_tokens"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "sudosleep_jwt_master_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128

        private const val KEY_ACCESS_TOKEN = "access_token_enc"
        private const val KEY_REFRESH_TOKEN = "refresh_token_enc"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ACTIVE_SEMESTER_ID = "active_semester_id"
    }
}
