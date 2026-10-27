package com.sudosleep.backend.dto;

import com.sudosleep.backend.entity.WeekParity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record TimetableEntryRequest(
        @NotNull @Min(1) @Max(7) @Schema(description = "1 = Monday ... 7 = Sunday", example = "1") Integer dayOfWeek,
        @NotNull @Schema(type = "string", example = "08:00") LocalTime startTime,
        @NotNull @Schema(type = "string", example = "10:00") LocalTime endTime,
        @Min(1) @Schema(description = "First week of the activity; defaults to 1", example = "1") Integer weekFrom,
        @Min(1) @Schema(description = "Last week of the activity; defaults to the last week of the semester", example = "10") Integer weekTo,
        @Schema(description = "ALL, ODD or EVEN weeks inside the range; defaults to ALL") WeekParity weekParity,
        @Size(max = 100) String room,
        @Size(max = 150) String teacher) {
}
