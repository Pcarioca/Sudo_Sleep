package com.sudosleep.backend.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user,
        Long activeSemesterId) {
}
