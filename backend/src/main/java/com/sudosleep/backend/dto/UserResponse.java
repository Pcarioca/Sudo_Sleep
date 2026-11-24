package com.sudosleep.backend.dto;

import com.sudosleep.backend.entity.AuthProvider;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        AuthProvider authProvider,
        boolean hasCompletedOnboarding,
        String defaultReminderOffsetsCsv,
        Instant createdAt) {
}
