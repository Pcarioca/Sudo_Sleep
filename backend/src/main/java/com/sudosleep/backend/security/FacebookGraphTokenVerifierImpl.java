package com.sudosleep.backend.security;

import com.sudosleep.backend.exception.BadRequestException;
import com.sudosleep.backend.exception.UnauthorizedException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Verifies Facebook user access tokens server-side against Meta Graph API:
 * <ol>
 *   <li>Inspects the token with {@code /debug_token} using {@code appId|appSecret} to confirm {@code is_valid}
 *       and that {@code app_id} belongs to this application.</li>
 *   <li>Fetches {@code /me?fields=id,name,email} using {@code appsecret_proof} (HMAC-SHA256).</li>
 * </ol>
 */
@Component
public class FacebookGraphTokenVerifierImpl implements FacebookTokenVerifier {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final OAuthProperties properties;
    private final RestClient restClient;

    public FacebookGraphTokenVerifierImpl(OAuthProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SocialUserInfo verify(String accessToken) {
        String appId = properties.facebook().appId();
        String appSecret = properties.facebook().appSecret();
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new BadRequestException(
                    "Facebook Login is not configured on this server (set FACEBOOK_APP_ID and FACEBOOK_APP_SECRET)");
        }
        try {
            Map<String, Object> debugResp = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/debug_token")
                            .queryParam("input_token", accessToken)
                            .queryParam("access_token", appId.trim() + "|" + appSecret.trim())
                            .build())
                    .retrieve()
                    .body(MAP_TYPE);

            Object dataObj = debugResp != null ? debugResp.get("data") : null;
            if (!(dataObj instanceof Map<?, ?> data)) {
                throw new UnauthorizedException("Invalid Facebook token inspection response");
            }
            boolean isValid = Boolean.TRUE.equals(data.get("is_valid"));
            String tokenAppId = data.get("app_id") != null ? String.valueOf(data.get("app_id")) : null;
            if (!isValid || !appId.trim().equals(tokenAppId)) {
                throw new UnauthorizedException("Invalid or expired Facebook access token");
            }

            String proof = hmacSha256Hex(accessToken, appSecret.trim());
            Map<String, Object> profile = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/me")
                            .queryParam("fields", "id,name,email")
                            .queryParam("access_token", accessToken)
                            .queryParam("appsecret_proof", proof)
                            .build())
                    .retrieve()
                    .body(MAP_TYPE);

            if (profile == null || profile.get("id") == null) {
                throw new UnauthorizedException("Could not load Facebook user profile");
            }
            String fbId = String.valueOf(profile.get("id"));
            String email = profile.get("email") != null ? String.valueOf(profile.get("email")) : null;
            String name = profile.get("name") != null ? String.valueOf(profile.get("name")) : "Facebook User";
            return new SocialUserInfo(fbId, email, email != null && !email.isBlank(), name);
        } catch (UnauthorizedException | BadRequestException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new UnauthorizedException("Failed to verify Facebook access token with Meta Graph API");
        }
    }

    static String hmacSha256Hex(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 unavailable", ex);
        }
    }
}
