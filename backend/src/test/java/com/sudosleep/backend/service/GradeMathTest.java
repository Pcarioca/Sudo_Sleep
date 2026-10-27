package com.sudosleep.backend.service;

import com.sudosleep.backend.entity.Grade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GradeMathTest {

    private static Grade grade(String score, String weight) {
        return new Grade(null, new BigDecimal(score), new BigDecimal(weight));
    }

    @Test
    void emptyListGivesNull() {
        assertNull(GradeMath.arithmeticAverage(List.of()));
        assertNull(GradeMath.weightedAverage(List.of()));
    }

    @Test
    void arithmeticAverageIgnoresWeights() {
        List<Grade> grades = List.of(grade("9.50", "1.00"), grade("10.00", "1.50"));
        assertEquals(new BigDecimal("9.75"), GradeMath.arithmeticAverage(grades));
    }

    @Test
    void weightedAverageUsesWeights() {
        // (9.5 * 1 + 10 * 1.5) / 2.5 = 9.8
        List<Grade> grades = List.of(grade("9.50", "1.00"), grade("10.00", "1.50"));
        assertEquals(new BigDecimal("9.80"), GradeMath.weightedAverage(grades));
    }

    @Test
    void equalWeightsGiveTheArithmeticAverage() {
        List<Grade> grades = List.of(grade("8.50", "1.00"), grade("9.00", "1.00"));
        assertEquals(new BigDecimal("8.75"), GradeMath.weightedAverage(grades));
        assertEquals(new BigDecimal("8.75"), GradeMath.arithmeticAverage(grades));
    }

    @Test
    void averageIsRoundedHalfUpToTwoDecimals() {
        // (7 + 8 + 8) / 3 = 7.666... -> 7.67
        List<Grade> grades = List.of(grade("7.00", "1.00"), grade("8.00", "1.00"), grade("8.00", "1.00"));
        assertEquals(new BigDecimal("7.67"), GradeMath.weightedAverage(grades));
    }

    @Test
    void zeroTotalWeightFallsBackToArithmeticAverage() {
        List<Grade> grades = List.of(grade("6.00", "0.00"), grade("8.00", "0.00"));
        assertEquals(new BigDecimal("7.00"), GradeMath.weightedAverage(grades));
    }

    @Test
    void gpaAveragesTheSubjectAverages() {
        List<Grade> subject1 = List.of(grade("9.50", "1.00"), grade("10.00", "1.50")); // 9.80
        List<Grade> subject2 = List.of(grade("8.50", "1.00"), grade("9.00", "1.00"));  // 8.75
        List<Grade> subject3 = List.of(grade("10.00", "2.00"));                         // 10.00
        // (9.8 + 8.75 + 10) / 3 = 9.5166... -> 9.52
        assertEquals(new BigDecimal("9.52"), GradeMath.gpa(List.of(subject1, subject2, subject3)));
    }

    @Test
    void gpaIgnoresSubjectsWithoutGrades() {
        List<Grade> subject1 = List.of(grade("9.00", "1.00"));
        assertEquals(new BigDecimal("9.00"), GradeMath.gpa(List.of(subject1, List.of())));
    }

    @Test
    void gpaWithoutAnyGradesIsNull() {
        assertNull(GradeMath.gpa(List.of(List.of(), List.of())));
        assertNull(GradeMath.gpa(List.of()));
    }
}
