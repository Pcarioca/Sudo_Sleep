package com.sudosleep.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record SemesterResponse(
        Long id,
        Long userId,
        String name,
        LocalDate startDate,
        int weeksCount,
        @Schema(description = "Current week number according to the server clock; null before the start or after the end of the semester")
        Integer currentWeek) {
}
