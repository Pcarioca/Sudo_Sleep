package com.sudosleep.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank @Schema(description = "Google OIDC ID token returned by Android Credential Manager") String idToken) {
}
