package com.sudosleep.backend.dto;

import com.sudosleep.backend.entity.ActivityType;
import com.sudosleep.backend.entity.WeekParity;

import java.time.LocalTime;
import java.util.List;

/** A timetable slot with the subject data, reminders, and single-week cancellations the grid needs. */
public record TimetableEntryResponse(
        Long id,
        Long requirementId,
        Long subjectId,
        String subjectName,
        String colorHex,
        ActivityType activityType,
        String customTypeName,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int weekFrom,
        int weekTo,
        WeekParity weekParity,
        String room,
        String teacher,
        String notes,
        boolean notificationsEnabled,
        String reminderOffsetsCsv,
        List<Integer> canceledIsoWeeks) {
}
