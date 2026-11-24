package com.sudosleep.backend.dto;

import com.sudosleep.backend.entity.AcademicEventType;

import java.time.LocalDate;
import java.time.LocalTime;

public record AcademicEventResponse(
        Long id,
        Long semesterId,
        Long subjectId,
        String subjectName,
        String title,
        AcademicEventType eventType,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String room,
        String notes,
        String colorHex,
        boolean notificationsEnabled,
        String reminderOffsetsCsv) {
}
