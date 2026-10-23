package com.example.sudosleep

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictDetectionTest {

    private fun isOverlap(start1: Int, dur1: Int, start2: Int, dur2: Int): Boolean {
        val end1 = start1 + dur1
        val end2 = start2 + dur2
        return start1 < end2 && end1 > start2
    }

    @Test
    fun testAdjacentSlotsDoNotOverlap() {
        // 08:00 - 10:00 and 10:00 - 12:00
        val result = isOverlap(8, 2, 10, 2)
        assertFalse("Adjacent time slots should not trigger an overlap conflict", result)
    }

    @Test
    fun testOverlappingSlotsConflict() {
        // 08:00 - 10:00 and 09:00 - 11:00
        val result = isOverlap(8, 2, 9, 2)
        assertTrue("Overlapping time slots must trigger conflict", result)
    }

    @Test
    fun testContainedSlotConflicts() {
        // 08:00 - 12:00 and 09:00 - 10:00
        val result = isOverlap(8, 4, 9, 1)
        assertTrue("Contained time slot must trigger conflict", result)
    }

    @Test
    fun testDifferentHoursNoConflict() {
        // 08:00 - 09:00 and 14:00 - 16:00
        val result = isOverlap(8, 1, 14, 2)
        assertFalse("Separated time slots must not conflict", result)
    }
}
