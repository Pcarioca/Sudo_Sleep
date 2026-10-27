package com.sudosleep.backend.service;

import com.sudosleep.backend.entity.Grade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

/** Grade averages. Results have 2 decimals; an empty input gives {@code null}. */
public final class GradeMath {

    private static final int RESULT_SCALE = 2;
    private static final int WORK_SCALE = 10;

    private GradeMath() {
    }

    /** Sum of scores / number of grades. */
    public static BigDecimal arithmeticAverage(List<Grade> grades) {
        BigDecimal value = arithmeticPrecise(grades);
        return value == null ? null : value.setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    /** Sum of (score * weight) / sum of weights. */
    public static BigDecimal weightedAverage(List<Grade> grades) {
        BigDecimal value = weightedPrecise(grades);
        return value == null ? null : value.setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Semester GPA: the average of the weighted averages of the subjects that have grades.
     * Subjects without grades are ignored; {@code null} if no subject has grades.
     */
    public static BigDecimal gpa(Collection<List<Grade>> gradesPerSubject) {
        BigDecimal sum = BigDecimal.ZERO;
        int subjects = 0;
        for (List<Grade> grades : gradesPerSubject) {
            BigDecimal average = weightedPrecise(grades);
            if (average != null) {
                sum = sum.add(average);
                subjects++;
            }
        }
        if (subjects == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(subjects), WORK_SCALE, RoundingMode.HALF_UP)
                .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal arithmeticPrecise(List<Grade> grades) {
        if (grades.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Grade grade : grades) {
            sum = sum.add(grade.getScore());
        }
        return sum.divide(BigDecimal.valueOf(grades.size()), WORK_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal weightedPrecise(List<Grade> grades) {
        if (grades.isEmpty()) {
            return null;
        }
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        for (Grade grade : grades) {
            weightedSum = weightedSum.add(grade.getScore().multiply(grade.getWeight()));
            weightSum = weightSum.add(grade.getWeight());
        }
        if (weightSum.signum() <= 0) {
            return arithmeticPrecise(grades);
        }
        return weightedSum.divide(weightSum, WORK_SCALE, RoundingMode.HALF_UP);
    }
}
