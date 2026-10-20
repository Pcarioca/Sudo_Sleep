package com.example.sudosleep

import com.example.sudosleep.data.local.entity.GradeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GradeCalculationTest {

    private fun calculateAverage(grades: List<GradeEntity>): Double {
        if (grades.isEmpty()) return 0.0
        val totalWeight = grades.sumOf { it.weight }
        return if (totalWeight > 0.0) {
            grades.sumOf { it.value * it.weight } / totalWeight
        } else {
            grades.sumOf { it.value } / grades.size
        }
    }

    @Test
    fun testEmptyGradesReturnsZero() {
        val avg = calculateAverage(emptyList())
        assertEquals(0.0, avg, 0.001)
    }

    @Test
    fun testArithmeticAverage() {
        val grades = listOf(
            GradeEntity(id = 1, subjectId = 1, value = 8.0, weight = 1.0),
            GradeEntity(id = 2, subjectId = 1, value = 10.0, weight = 1.0)
        )
        val avg = calculateAverage(grades)
        assertEquals(9.0, avg, 0.001)
    }

    @Test
    fun testWeightedAverage() {
        val grades = listOf(
            GradeEntity(id = 1, subjectId = 1, value = 8.0, weight = 1.0), // 8 * 1 = 8
            GradeEntity(id = 2, subjectId = 1, value = 10.0, weight = 3.0) // 10 * 3 = 30 -> sum 38 / 4 = 9.5
        )
        val avg = calculateAverage(grades)
        assertEquals(9.5, avg, 0.001)
    }
}
