package com.sudosleep.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradeRequest(
        @NotNull @DecimalMin("1.00") @DecimalMax("10.00") @Digits(integer = 2, fraction = 2)
        @Schema(example = "9.50") BigDecimal score,
        @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 3, fraction = 2)
        @Schema(description = "Defaults to 1.00", example = "1.50") BigDecimal weight) {
}
