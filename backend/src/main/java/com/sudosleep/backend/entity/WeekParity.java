package com.sudosleep.backend.entity;

/** Which weeks of a week range an activity runs in. */
public enum WeekParity {
    ALL,
    ODD,
    EVEN;

    /** True if an activity with this parity takes place in the given week (1-based). */
    public boolean matches(int week) {
        return switch (this) {
            case ALL -> true;
            case ODD -> week % 2 != 0;
            case EVEN -> week % 2 == 0;
        };
    }
}
