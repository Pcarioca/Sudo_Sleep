package com.sudosleep.backend.dto;

import com.sudosleep.backend.entity.ActivityType;
import com.sudosleep.backend.entity.WeekParity;

import java.time.LocalTime;

/** A timetable slot with the subject data the grid needs (name, color, activity type). */
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
        String teacher) {
}
