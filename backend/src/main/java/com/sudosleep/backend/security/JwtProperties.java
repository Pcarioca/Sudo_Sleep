package com.sudosleep.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessExpirationSeconds,
        long refreshExpirationSeconds) {

    public JwtProperties {
        if (accessExpirationSeconds <= 0) {
            accessExpirationSeconds = 3600L;
        }
        if (refreshExpirationSeconds <= 0) {
            refreshExpirationSeconds = 2_592_000L;
        }
    }
}
