package com.sudosleep.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.sudosleep.backend.entity.User;
import com.sudosleep.backend.exception.UnauthorizedException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Signs and validates stateless HS256 JWT access and refresh tokens. */
@Service
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String ISSUER = "sudosleep-api";

    private final JwtProperties properties;
    private final Clock clock;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        SecretKey secretKey = deriveHmacKey(properties.secret());
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(clock);
        nimbusDecoder.setJwtValidator(timestampValidator);
        this.decoder = nimbusDecoder;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, TOKEN_TYPE_ACCESS, properties.accessExpirationSeconds());
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, TOKEN_TYPE_REFRESH, properties.refreshExpirationSeconds());
    }

    public long accessExpirationSeconds() {
        return properties.accessExpirationSeconds();
    }

    public AuthenticatedUser parseAndValidate(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Missing authentication token");
        }
        try {
            Jwt jwt = decoder.decode(token);
            String tokenType = jwt.getClaimAsString("type");
            if (!expectedType.equals(tokenType)) {
                throw new UnauthorizedException("Invalid token type");
            }
            Instant exp = jwt.getExpiresAt();
            if (exp == null || !exp.isAfter(Instant.now(clock))) {
                throw new UnauthorizedException("Token has expired");
            }
            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            if (subject == null || email == null) {
                throw new UnauthorizedException("Malformed authentication token");
            }
            return new AuthenticatedUser(Long.parseLong(subject), email);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (JwtException | NumberFormatException ex) {
            throw new UnauthorizedException("Invalid or expired authentication token");
        }
    }

    private String generateToken(User user, String type, long ttlSeconds) {
        Instant now = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("type", type)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static SecretKey deriveHmacKey(String rawSecret) {
        String material = (rawSecret == null || rawSecret.isBlank())
                ? "dev-only-insecure-secret-change-me-at-least-32-bytes-long"
                : rawSecret;
        byte[] bytes = material.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            try {
                bytes = MessageDigest.getInstance("SHA-256").digest(bytes);
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("SHA-256 unavailable", ex);
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
