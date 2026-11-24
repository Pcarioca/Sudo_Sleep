package com.sudosleep.backend.security;

/** Verified identity profile extracted from a Google ID token or Facebook access token. */
public record SocialUserInfo(
        String providerUserId,
        String email,
        boolean emailVerified,
        String displayName) {
}
