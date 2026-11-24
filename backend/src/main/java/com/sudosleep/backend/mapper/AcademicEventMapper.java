package com.sudosleep.backend.mapper;

import com.sudosleep.backend.dto.AcademicEventResponse;
import com.sudosleep.backend.entity.AcademicEvent;
import com.sudosleep.backend.entity.Subject;
import org.springframework.stereotype.Component;

@Component
public class AcademicEventMapper {

    public AcademicEventResponse toResponse(AcademicEvent event) {
        Subject subject = event.getSubject();
        return new AcademicEventResponse(
                event.getId(),
                event.getSemester().getId(),
                subject != null ? subject.getId() : null,
                subject != null ? subject.getName() : null,
                event.getTitle(),
                event.getEventType(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getRoom(),
                event.getNotes(),
                event.getColorHex(),
                event.isNotificationsEnabled(),
                event.getReminderOffsetsCsv());
    }
}
