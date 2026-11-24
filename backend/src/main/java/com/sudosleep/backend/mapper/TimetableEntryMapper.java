package com.sudosleep.backend.mapper;

import com.sudosleep.backend.dto.TimetableEntryResponse;
import com.sudosleep.backend.entity.ActivityRequirement;
import com.sudosleep.backend.entity.Subject;
import com.sudosleep.backend.entity.TimetableEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TimetableEntryMapper {

    public TimetableEntryResponse toResponse(TimetableEntry entry, List<Integer> canceledIsoWeeks) {
        ActivityRequirement requirement = entry.getRequirement();
        Subject subject = requirement.getSubject();
        return new TimetableEntryResponse(
                entry.getId(),
                requirement.getId(),
                subject.getId(),
                subject.getName(),
                subject.getColorHex(),
                requirement.getActivityType(),
                requirement.getCustomTypeName(),
                entry.getDayOfWeek(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getWeekFrom(),
                entry.getWeekTo(),
                entry.getWeekParity(),
                entry.getRoom(),
                entry.getTeacher(),
                entry.getNotes(),
                entry.isNotificationsEnabled(),
                entry.getReminderOffsetsCsv(),
                canceledIsoWeeks == null ? List.of() : List.copyOf(canceledIsoWeeks));
    }
}
