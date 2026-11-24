package com.sudosleep.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for updating the authenticated user's profile and optional notification/onboarding settings. */
public record UserProfileUpdateRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Email @Size(max = 255) String email,
        Boolean hasCompletedOnboarding,
        @Size(max = 64) String defaultReminderOffsetsCsv) {
}
