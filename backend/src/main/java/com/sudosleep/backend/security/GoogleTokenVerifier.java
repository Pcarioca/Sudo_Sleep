package com.sudosleep.backend.security;

/** Verifies a Google OIDC ID token and returns the authenticated Google user profile. */
public interface GoogleTokenVerifier {

    SocialUserInfo verify(String idToken);
}
