package com.sudosleep.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 255) @Schema(example = "alex@university.edu") String email,
        @NotBlank @Size(max = 128) String password) {
}
