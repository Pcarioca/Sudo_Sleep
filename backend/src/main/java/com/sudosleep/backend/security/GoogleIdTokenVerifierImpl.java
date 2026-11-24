package com.sudosleep.backend.security;

import com.sudosleep.backend.exception.BadRequestException;
import com.sudosleep.backend.exception.UnauthorizedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Verifies Google OIDC ID tokens against Google's official JWKS endpoint,
 * checking signature, expiration, issuer, and audience ({@code GOOGLE_WEB_CLIENT_ID}).
 */
@Component
public class GoogleIdTokenVerifierImpl implements GoogleTokenVerifier {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> VALID_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com");

    private final OAuthProperties properties;
    private final JwtDecoder googleJwtDecoder;

    public GoogleIdTokenVerifierImpl(OAuthProperties properties) {
        this.properties = properties;
        this.googleJwtDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
    }

    @Override
    public SocialUserInfo verify(String idToken) {
        String webClientId = properties.google().webClientId();
        if (webClientId == null || webClientId.isBlank()) {
            throw new BadRequestException(
                    "Google Sign-In is not configured on this server (set GOOGLE_WEB_CLIENT_ID)");
        }
        try {
            Jwt jwt = googleJwtDecoder.decode(idToken);
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : jwt.getClaimAsString("iss");
            if (issuer == null || !VALID_ISSUERS.contains(issuer)) {
                throw new UnauthorizedException("Invalid Google ID token issuer");
            }
            List<String> audiences = jwt.getAudience();
            if (audiences == null || !audiences.contains(webClientId.trim())) {
                throw new UnauthorizedException("Google ID token audience does not match this application");
            }
            String sub = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
                throw new UnauthorizedException("Google ID token is missing required subject or email claims");
            }
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            String name = jwt.getClaimAsString("name");
            if (name == null || name.isBlank()) {
                name = email.substring(0, email.indexOf('@'));
            }
            return new SocialUserInfo(sub, email, Boolean.TRUE.equals(emailVerified), name);
        } catch (UnauthorizedException | BadRequestException ex) {
            throw ex;
        } catch (JwtException ex) {
            throw new UnauthorizedException("Invalid or expired Google ID token");
        }
    }
}
