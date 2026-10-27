package com.sudosleep.backend.mapper;

import com.sudosleep.backend.dto.TimetableEntryResponse;
import com.sudosleep.backend.entity.ActivityRequirement;
import com.sudosleep.backend.entity.Subject;
import com.sudosleep.backend.entity.TimetableEntry;
import org.springframework.stereotype.Component;

@Component
public class TimetableEntryMapper {

    public TimetableEntryResponse toResponse(TimetableEntry entry) {
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
                entry.getTeacher());
    }
}
