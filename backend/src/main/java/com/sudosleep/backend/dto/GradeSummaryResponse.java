package com.sudosleep.backend.dto;

import java.math.BigDecimal;

public record GradeSummaryResponse(
        Long subjectId,
        int gradeCount,
        BigDecimal arithmeticAverage,
        BigDecimal weightedAverage) {
}
