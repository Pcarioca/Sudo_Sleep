package com.sudosleep.backend.dto;

import java.math.BigDecimal;

public record GradeResponse(Long id, Long subjectId, BigDecimal score, BigDecimal weight) {
}
