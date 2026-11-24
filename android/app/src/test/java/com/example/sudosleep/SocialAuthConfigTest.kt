package com.example.sudosleep

import com.example.sudosleep.data.auth.SocialAuthConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialAuthConfigTest {

    @Test
    fun isConfiguredValueRejectsBlankAndPlaceholders() {
        assertFalse(SocialAuthConfig.isConfiguredValue(null))
        assertFalse(SocialAuthConfig.isConfiguredValue(""))
        assertFalse(SocialAuthConfig.isConfiguredValue("   "))
        assertFalse(SocialAuthConfig.isConfiguredValue("YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"))
        assertFalse(SocialAuthConfig.isConfiguredValue("your-facebook-app-id"))
        assertFalse(SocialAuthConfig.isConfiguredValue("fbYOUR_FACEBOOK_APP_ID"))
        assertFalse(SocialAuthConfig.isConfiguredValue("<replace-me>"))
        assertTrue(SocialAuthConfig.isConfiguredValue("1234567890-abc.apps.googleusercontent.com"))
        assertTrue(SocialAuthConfig.isConfiguredValue("123456789012345"))
    }

    @Test
    fun resolveGoogleWebClientIdPrefersBuildConfigAndFallsBackToStringResource() {
        assertEquals(
            "build-config-id.apps.googleusercontent.com",
            SocialAuthConfig.resolveGoogleWebClientId(
                buildConfigValue = "build-config-id.apps.googleusercontent.com",
                resourceValue = "resource-id.apps.googleusercontent.com"
            )
        )
        assertEquals(
            "resource-id.apps.googleusercontent.com",
            SocialAuthConfig.resolveGoogleWebClientId(
                buildConfigValue = "your-google-web-client-id.apps.googleusercontent.com",
                resourceValue = "resource-id.apps.googleusercontent.com"
            )
        )
        assertNull(
            SocialAuthConfig.resolveGoogleWebClientId(
                buildConfigValue = "",
                resourceValue = "YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"
            )
        )
    }

    @Test
    fun isFacebookConfiguredValidatesAllFieldsAndProtocolSchemeMatch() {
        assertTrue(
            SocialAuthConfig.isFacebookConfigured(
                appId = "987654321098765",
                clientToken = "abcdef1234567890abcdef",
                protocolScheme = "fb987654321098765"
            )
        )
        assertFalse(
            SocialAuthConfig.isFacebookConfigured(
                appId = "987654321098765",
                clientToken = "abcdef1234567890abcdef",
                protocolScheme = "fb000000000000000"
            )
        )
        assertFalse(
            SocialAuthConfig.isFacebookConfigured(
                appId = "YOUR_FACEBOOK_APP_ID",
                clientToken = "YOUR_FACEBOOK_CLIENT_TOKEN",
                protocolScheme = "fbYOUR_FACEBOOK_APP_ID"
            )
        )
    }

    @Test
    fun generateNonceProducesUniqueUrlSafeStrings() {
        val nonce1 = SocialAuthConfig.generateNonce()
        val nonce2 = SocialAuthConfig.generateNonce()
        assertTrue(nonce1.length >= 32)
        assertNotEquals(nonce1, nonce2)
        assertFalse(nonce1.contains("="))
        assertFalse(nonce1.contains("+"))
        assertFalse(nonce1.contains("/"))
    }
}
