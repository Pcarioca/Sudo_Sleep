package com.sudosleep.backend.exception;

import com.sudosleep.backend.dto.TimetableEntryResponse;

import java.util.List;

/**
 * The slot overlaps existing timetable entries (HTTP 409). The client may repeat the
 * request with {@code force=true} to schedule anyway.
 */
public class ScheduleConflictException extends RuntimeException {

    private final List<TimetableEntryResponse> conflicts;

    public ScheduleConflictException(List<TimetableEntryResponse> conflicts) {
        super("The slot overlaps " + conflicts.size() + " existing timetable entr"
                + (conflicts.size() == 1 ? "y" : "ies") + ". Repeat the request with force=true to schedule anyway.");
        this.conflicts = List.copyOf(conflicts);
    }

    public List<TimetableEntryResponse> getConflicts() {
        return conflicts;
    }
}
