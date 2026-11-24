package com.sudosleep.backend.dto;

import com.sudosleep.backend.entity.AcademicEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record AcademicEventRequest(
        @Schema(description = "Optional linked subject ID (null for standalone/general events)") Long subjectId,
        @NotBlank @Size(max = 150) @Schema(example = "Mobile Systems Midterm Exam") String title,
        @NotNull AcademicEventType eventType,
        @NotNull @Schema(example = "2026-11-18") LocalDate eventDate,
        @NotNull @Schema(type = "string", example = "10:00") LocalTime startTime,
        @NotNull @Schema(type = "string", example = "12:00") LocalTime endTime,
        @Size(max = 100) String room,
        @Size(max = 500) String notes,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must look like #D81B60")
        @Schema(example = "#D81B60") String colorHex,
        Boolean notificationsEnabled,
        @Size(max = 64) @Schema(example = "15,60,1440") String reminderOffsetsCsv) {
}
