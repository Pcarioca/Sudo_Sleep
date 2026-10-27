package com.sudosleep.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubjectRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must look like #0288D1")
        @Schema(example = "#0288D1") String colorHex,
        @Valid @Schema(description = "Optional: required activities created together with the subject")
        List<ActivityRequirementRequest> requirements) {
}
