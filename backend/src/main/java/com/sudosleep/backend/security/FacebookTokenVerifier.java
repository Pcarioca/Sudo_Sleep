package com.sudosleep.backend.security;

/** Verifies a Facebook user access token via Meta Graph API and returns the authenticated profile. */
public interface FacebookTokenVerifier {

    SocialUserInfo verify(String accessToken);
}
