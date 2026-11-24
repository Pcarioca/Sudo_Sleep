package com.sudosleep.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Alex Rivera") String displayName,
        @NotBlank @Email @Size(max = 255) @Schema(example = "alex@university.edu") String email,
        @NotBlank @Size(min = 8, max = 128) @Schema(example = "Str0ngPass!") String password) {
}
