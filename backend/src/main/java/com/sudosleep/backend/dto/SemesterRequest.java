package com.sudosleep.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SemesterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Schema(description = "Monday of week 1", example = "2026-10-05") LocalDate startDate,
        @Min(1) @Max(52) @Schema(description = "Number of weeks, defaults to 14", example = "14") Integer weeksCount) {
}
