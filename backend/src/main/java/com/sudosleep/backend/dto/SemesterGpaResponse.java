package com.sudosleep.backend.dto;

import java.math.BigDecimal;

public record SemesterGpaResponse(
        Long semesterId,
        int subjectsWithGrades,
        /** Average of the weighted averages of the subjects that have grades; null if there are none. */
        BigDecimal gpa) {
}
