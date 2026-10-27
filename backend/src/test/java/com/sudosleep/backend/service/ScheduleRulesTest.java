package com.sudosleep.backend.service;

import com.sudosleep.backend.entity.WeekParity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleRulesTest {

    private static LocalTime t(int hour) {
        return LocalTime.of(hour, 0);
    }

    // ---- time overlap: newStart < existingEnd && newEnd > existingStart ----

    @Test
    void adjacentSlotsDoNotOverlap() {
        assertFalse(ScheduleRules.timesOverlap(t(8), t(10), t(10), t(12)));
        assertFalse(ScheduleRules.timesOverlap(t(10), t(12), t(8), t(10)));
    }

    @Test
    void partiallyOverlappingSlotsOverlap() {
        assertTrue(ScheduleRules.timesOverlap(t(8), t(10), t(9), t(11)));
        assertTrue(ScheduleRules.timesOverlap(t(9), t(11), t(8), t(10)));
    }

    @Test
    void containedSlotOverlaps() {
        assertTrue(ScheduleRules.timesOverlap(t(8), t(12), t(9), t(10)));
        assertTrue(ScheduleRules.timesOverlap(t(9), t(10), t(8), t(12)));
    }

    @Test
    void separatedSlotsDoNotOverlap() {
        assertFalse(ScheduleRules.timesOverlap(t(8), t(9), t(14), t(16)));
    }

    // ---- weeks ----

    @Test
    void parityMatchesWeeks() {
        assertTrue(WeekParity.ALL.matches(1));
        assertTrue(WeekParity.ALL.matches(2));
        assertTrue(WeekParity.ODD.matches(1));
        assertTrue(WeekParity.ODD.matches(13));
        assertFalse(WeekParity.ODD.matches(2));
        assertTrue(WeekParity.EVEN.matches(2));
        assertFalse(WeekParity.EVEN.matches(7));
    }

    @Test
    void occursInWeekChecksRangeAndParity() {
        // course from week 1 to week 10
        assertTrue(ScheduleRules.occursInWeek(1, 10, WeekParity.ALL, 1));
        assertTrue(ScheduleRules.occursInWeek(1, 10, WeekParity.ALL, 10));
        assertFalse(ScheduleRules.occursInWeek(1, 10, WeekParity.ALL, 11));
        // odd weeks only
        assertTrue(ScheduleRules.occursInWeek(1, 14, WeekParity.ODD, 5));
        assertFalse(ScheduleRules.occursInWeek(1, 14, WeekParity.ODD, 6));
        // before the range
        assertFalse(ScheduleRules.occursInWeek(3, 14, WeekParity.ALL, 2));
    }

    @Test
    void oddAndEvenActivitiesNeverShareAWeek() {
        assertFalse(ScheduleRules.shareAWeek(1, 14, WeekParity.ODD, 1, 14, WeekParity.EVEN));
    }

    @Test
    void allWeeksSharesWithOddWeeks() {
        assertTrue(ScheduleRules.shareAWeek(1, 14, WeekParity.ALL, 1, 14, WeekParity.ODD));
    }

    @Test
    void disjointRangesDoNotShareAWeek() {
        assertFalse(ScheduleRules.shareAWeek(1, 7, WeekParity.ALL, 8, 14, WeekParity.ALL));
    }

    @Test
    void rangesTouchingInOneWeekShareIt() {
        assertTrue(ScheduleRules.shareAWeek(1, 5, WeekParity.ALL, 5, 10, WeekParity.ALL));
    }

    @Test
    void intersectingRangesNeedACommonParityWeek() {
        // ranges intersect only in week 2, which is not an odd week
        assertFalse(ScheduleRules.shareAWeek(1, 2, WeekParity.ODD, 2, 3, WeekParity.ODD));
        // here they intersect in weeks 2..3 and week 3 is odd
        assertTrue(ScheduleRules.shareAWeek(1, 3, WeekParity.ODD, 2, 3, WeekParity.ODD));
    }

    @Test
    void sameRangeSameParitySharesAWeek() {
        assertTrue(ScheduleRules.shareAWeek(2, 12, WeekParity.EVEN, 2, 12, WeekParity.EVEN));
    }

    @Test
    void hasAnyWeekRejectsRangesThatNeverHappen() {
        assertFalse(ScheduleRules.hasAnyWeek(2, 2, WeekParity.ODD));
        assertTrue(ScheduleRules.hasAnyWeek(2, 2, WeekParity.EVEN));
        assertTrue(ScheduleRules.hasAnyWeek(3, 3, WeekParity.ALL));
        assertTrue(ScheduleRules.hasAnyWeek(1, 2, WeekParity.ODD));
    }

    // ---- current week ----

    @Test
    void weekNumberCountsFromTheStartMonday() {
        LocalDate start = LocalDate.of(2026, 10, 5); // a Monday
        assertEquals(1, ScheduleRules.weekNumber(start, start));
        assertEquals(1, ScheduleRules.weekNumber(start, start.plusDays(6)));
        assertEquals(2, ScheduleRules.weekNumber(start, start.plusDays(7)));
        assertEquals(3, ScheduleRules.weekNumber(start, LocalDate.of(2026, 10, 23)));
        assertEquals(14, ScheduleRules.weekNumber(start, start.plusWeeks(13)));
    }

    @Test
    void weekNumberBeforeTheStartIsBelowOne() {
        LocalDate start = LocalDate.of(2026, 10, 5);
        assertEquals(0, ScheduleRules.weekNumber(start, start.minusDays(1)));
        assertEquals(0, ScheduleRules.weekNumber(start, start.minusDays(7)));
        assertEquals(-1, ScheduleRules.weekNumber(start, start.minusDays(8)));
    }
}
