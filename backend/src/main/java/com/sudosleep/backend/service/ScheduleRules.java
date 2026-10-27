package com.sudosleep.backend.service;

import com.sudosleep.backend.entity.WeekParity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Pure scheduling rules, no Spring or database involved (easy to unit test).
 *
 * <p>Two timetable entries conflict when ALL of these hold:
 * <ol>
 *   <li>they are on the same day of the week;</li>
 *   <li>their times overlap: {@code newStart < existingEnd && newEnd > existingStart};</li>
 *   <li>they share at least one week (week ranges intersect <em>and</em> the parities allow a common week).</li>
 * </ol>
 * The first rule is applied by the repository query, the other two by {@link #timesOverlap}
 * and {@link #shareAWeek}.
 */
public final class ScheduleRules {

    /** The timetable grid starts at 08:00 ... */
    public static final LocalTime GRID_START = LocalTime.of(8, 0);
    /** ... and ends at 22:00. */
    public static final LocalTime GRID_END = LocalTime.of(22, 0);
    /** An activity lasts at most 4 hours. */
    public static final Duration MAX_DURATION = Duration.ofHours(4);

    private ScheduleRules() {
    }

    public static boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /** Does an activity running in weeks {@code weekFrom..weekTo} with this parity take place in {@code week}? */
    public static boolean occursInWeek(int weekFrom, int weekTo, WeekParity parity, int week) {
        return week >= weekFrom && week <= weekTo && parity.matches(week);
    }

    /** False for e.g. ODD weeks 2..2, which would never happen. */
    public static boolean hasAnyWeek(int weekFrom, int weekTo, WeekParity parity) {
        for (int week = weekFrom; week <= weekTo; week++) {
            if (parity.matches(week)) {
                return true;
            }
        }
        return false;
    }

    /** Is there at least one week in which both activities take place? */
    public static boolean shareAWeek(int from1, int to1, WeekParity parity1,
                                     int from2, int to2, WeekParity parity2) {
        int from = Math.max(from1, from2);
        int to = Math.min(to1, to2);
        for (int week = from; week <= to; week++) {
            if (parity1.matches(week) && parity2.matches(week)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Week number of {@code date} in a semester whose week 1 starts on {@code semesterStart}.
     * Values below 1 mean the date is before the semester.
     */
    public static int weekNumber(LocalDate semesterStart, LocalDate date) {
        long days = ChronoUnit.DAYS.between(semesterStart, date);
        return (int) Math.floorDiv(days, 7L) + 1;
    }
}
