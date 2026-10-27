package com.sudosleep.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record SubjectResponse(
        Long id,
        Long semesterId,
        String name,
        String colorHex,
        List<ActivityRequirementResponse> requirements,
        int gradeCount,
        /** Plain average of the scores; null when there are no grades. */
        BigDecimal arithmeticAverage,
        /** Average weighted by the grade weights; null when there are no grades. */
        BigDecimal weightedAverage) {
}
