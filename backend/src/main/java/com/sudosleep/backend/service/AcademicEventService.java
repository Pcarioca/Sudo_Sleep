package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.AcademicEventRequest;
import com.sudosleep.backend.dto.AcademicEventResponse;
import com.sudosleep.backend.entity.AcademicEvent;
import com.sudosleep.backend.entity.Semester;
import com.sudosleep.backend.entity.Subject;
import com.sudosleep.backend.exception.BadRequestException;
import com.sudosleep.backend.exception.ResourceNotFoundException;
import com.sudosleep.backend.mapper.AcademicEventMapper;
import com.sudosleep.backend.repository.AcademicEventRepository;
import com.sudosleep.backend.repository.SemesterRepository;
import com.sudosleep.backend.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AcademicEventService {

    private final AcademicEventRepository events;
    private final SemesterRepository semesters;
    private final SubjectRepository subjects;
    private final AcademicEventMapper mapper;

    public AcademicEventService(AcademicEventRepository events,
                                SemesterRepository semesters,
                                SubjectRepository subjects,
                                AcademicEventMapper mapper) {
        this.events = events;
        this.semesters = semesters;
        this.subjects = subjects;
        this.mapper = mapper;
    }

    public List<AcademicEventResponse> listForSemester(Long semesterId,
                                                       LocalDate fromDate,
                                                       LocalDate toDate,
                                                       Long currentUserId) {
        findOwnedSemester(semesterId, currentUserId);
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new BadRequestException("toDate must be on or after fromDate");
        }
        List<AcademicEvent> list = (fromDate == null && toDate == null)
                ? events.findAllBySemesterId(semesterId)
                : events.findBySemesterIdAndDateRange(semesterId, fromDate, toDate);
        return list.stream().map(mapper::toResponse).toList();
    }

    public AcademicEventResponse get(Long id, Long currentUserId) {
        return mapper.toResponse(findOwned(id, currentUserId));
    }

    @Transactional
    public AcademicEventResponse create(Long semesterId, Long currentUserId, AcademicEventRequest request) {
        Semester semester = findOwnedSemester(semesterId, currentUserId);
        AcademicEvent event = new AcademicEvent(semester);
        apply(event, semester, request);
        return mapper.toResponse(events.save(event));
    }

    @Transactional
    public AcademicEventResponse update(Long id, Long currentUserId, AcademicEventRequest request) {
        AcademicEvent event = findOwned(id, currentUserId);
        apply(event, event.getSemester(), request);
        return mapper.toResponse(event);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        events.delete(findOwned(id, currentUserId));
    }

    // ---- internals --------------------------------------------------------------------

    private void apply(AcademicEvent event, Semester semester, AcademicEventRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }
        Subject subject = resolveSubject(request.subjectId(), semester.getId());
        event.setSubject(subject);
        event.setTitle(request.title().trim());
        event.setEventType(request.eventType());
        event.setEventDate(request.eventDate());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setRoom(blankToNull(request.room()));
        event.setNotes(blankToNull(request.notes()));
        event.setColorHex(resolveColor(request.colorHex(), subject));
        event.setNotificationsEnabled(request.notificationsEnabled() != null ? request.notificationsEnabled() : true);
        event.setReminderOffsetsCsv(request.reminderOffsetsCsv() != null
                ? request.reminderOffsetsCsv().trim()
                : AcademicEvent.DEFAULT_REMINDER_OFFSETS);
    }

    private Subject resolveSubject(Long subjectId, Long semesterId) {
        if (subjectId == null) {
            return null;
        }
        Subject subject = subjects.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
        if (!subject.getSemester().getId().equals(semesterId)) {
            throw new BadRequestException("Subject " + subjectId + " does not belong to semester " + semesterId);
        }
        return subject;
    }

    private static String resolveColor(String colorHex, Subject subject) {
        if (colorHex != null && !colorHex.isBlank()) {
            return colorHex.trim().toUpperCase(Locale.ROOT);
        }
        if (subject != null && subject.getColorHex() != null) {
            return subject.getColorHex().toUpperCase(Locale.ROOT);
        }
        return AcademicEvent.DEFAULT_COLOR_HEX;
    }

    private Semester findOwnedSemester(Long semesterId, Long currentUserId) {
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", semesterId));
        OwnershipGuard.requireOwner(semester.getUser().getId(), currentUserId);
        return semester;
    }

    private AcademicEvent findOwned(Long id, Long currentUserId) {
        AcademicEvent event = events.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic event", id));
        OwnershipGuard.requireOwner(event.getSemester().getUser().getId(), currentUserId);
        return event;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
