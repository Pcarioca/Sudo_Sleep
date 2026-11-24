package com.sudosleep.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record FacebookAuthRequest(
        @NotBlank @Schema(description = "Facebook user access token returned by Facebook Login SDK") String accessToken) {
}
