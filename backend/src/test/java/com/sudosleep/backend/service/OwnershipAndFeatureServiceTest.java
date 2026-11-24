package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.AcademicEventRequest;
import com.sudosleep.backend.dto.AcademicEventResponse;
import com.sudosleep.backend.dto.TimetableEntryRequest;
import com.sudosleep.backend.dto.TimetableEntryResponse;
import com.sudosleep.backend.entity.AcademicEvent;
import com.sudosleep.backend.entity.AcademicEventType;
import com.sudosleep.backend.entity.ActivityRequirement;
import com.sudosleep.backend.entity.ActivityType;
import com.sudosleep.backend.entity.AuthProvider;
import com.sudosleep.backend.entity.Semester;
import com.sudosleep.backend.entity.Subject;
import com.sudosleep.backend.entity.TimetableEntry;
import com.sudosleep.backend.entity.TimetableEntryCancellation;
import com.sudosleep.backend.entity.User;
import com.sudosleep.backend.entity.WeekParity;
import com.sudosleep.backend.exception.BadRequestException;
import com.sudosleep.backend.exception.ForbiddenException;
import com.sudosleep.backend.mapper.AcademicEventMapper;
import com.sudosleep.backend.mapper.TimetableEntryMapper;
import com.sudosleep.backend.repository.AcademicEventRepository;
import com.sudosleep.backend.repository.ActivityRequirementRepository;
import com.sudosleep.backend.repository.SemesterRepository;
import com.sudosleep.backend.repository.SubjectRepository;
import com.sudosleep.backend.repository.TimetableEntryCancellationRepository;
import com.sudosleep.backend.repository.TimetableEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipAndFeatureServiceTest {

    @Mock private AcademicEventRepository eventRepo;
    @Mock private SemesterRepository semesterRepo;
    @Mock private SubjectRepository subjectRepo;
    @Mock private TimetableEntryRepository entryRepo;
    @Mock private TimetableEntryCancellationRepository cancellationRepo;
    @Mock private ActivityRequirementRepository requirementRepo;

    private AcademicEventService eventService;
    private TimetableService timetableService;

    private User owner;
    private Semester semester;
    private Subject subject;
    private ActivityRequirement requirement;

    @BeforeEach
    void setUp() {
        eventService = new AcademicEventService(eventRepo, semesterRepo, subjectRepo, new AcademicEventMapper());
        timetableService = new TimetableService(
                entryRepo, cancellationRepo, requirementRepo, semesterRepo, new TimetableEntryMapper());

        owner = new User("owner@uni.ro", "Owner", "hash", AuthProvider.LOCAL);
        ReflectionTestUtils.setField(owner, "id", 1L);

        semester = new Semester(owner, "Fall 2026", LocalDate.of(2026, 9, 28), 14);
        ReflectionTestUtils.setField(semester, "id", 10L);

        subject = new Subject(semester, "Algorithms", "#1E88E5");
        ReflectionTestUtils.setField(subject, "id", 20L);

        requirement = new ActivityRequirement(subject, ActivityType.LECTURE, null);
        ReflectionTestUtils.setField(requirement, "id", 30L);
    }

    @Test
    void academicEventCreateInheritsSubjectColorAndEnforcesOwnership() {
        when(semesterRepo.findById(10L)).thenReturn(Optional.of(semester));
        when(subjectRepo.findById(20L)).thenReturn(Optional.of(subject));
        when(eventRepo.save(any(AcademicEvent.class))).thenAnswer(inv -> {
            AcademicEvent e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 500L);
            return e;
        });

        AcademicEventRequest request = new AcademicEventRequest(
                20L,
                "Algorithms Midterm",
                AcademicEventType.MIDTERM,
                LocalDate.of(2026, 11, 12),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Aula A1",
                "Bring student ID",
                null,
                true,
                "15,60");

        // Another user cannot create an event in owner's semester
        assertThrows(ForbiddenException.class, () -> eventService.create(10L, 999L, request));

        // Owner succeeds and inherits subject color
        AcademicEventResponse created = eventService.create(10L, 1L, request);
        assertEquals(500L, created.id());
        assertEquals("#1E88E5", created.colorHex());
        assertEquals("Algorithms", created.subjectName());
        assertEquals("15,60", created.reminderOffsetsCsv());
    }

    @Test
    void academicEventRejectsInvalidTimeRange() {
        when(semesterRepo.findById(10L)).thenReturn(Optional.of(semester));
        AcademicEventRequest badTime = new AcademicEventRequest(
                null,
                "Bad Exam",
                AcademicEventType.EXAM,
                LocalDate.of(2026, 11, 12),
                LocalTime.of(14, 0),
                LocalTime.of(12, 0),
                null, null, null, null, null);

        assertThrows(BadRequestException.class, () -> eventService.create(10L, 1L, badTime));
    }

    @Test
    void timetableCancellationToggleAndWeekConflictFiltering() {
        TimetableEntry existing = new TimetableEntry(requirement);
        ReflectionTestUtils.setField(existing, "id", 40L);
        existing.setDayOfWeek(1);
        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(12, 0));
        existing.setWeekFrom(1);
        existing.setWeekTo(14);
        existing.setWeekParity(WeekParity.ALL);

        // Unauthorized user cannot cancel
        when(entryRepo.findDetailedById(40L)).thenReturn(Optional.of(existing));
        assertThrows(ForbiddenException.class, () -> timetableService.cancelForWeek(40L, 202648, 999L));

        // Invalid isoYearWeek rejected
        assertThrows(BadRequestException.class, () -> timetableService.cancelForWeek(40L, 202699, 1L));

        // Owner cancels week 202648
        when(cancellationRepo.existsByEntryIdAndIsoYearWeek(40L, 202648)).thenReturn(false);
        TimetableEntryCancellation savedCancellation = new TimetableEntryCancellation(existing, 202648);
        when(cancellationRepo.findByEntryIdOrderByIsoYearWeekAsc(40L)).thenReturn(List.of(savedCancellation));

        TimetableEntryResponse canceled = timetableService.cancelForWeek(40L, 202648, 1L);
        verify(cancellationRepo).save(any(TimetableEntryCancellation.class));
        assertEquals(List.of(202648), canceled.canceledIsoWeeks());

        // Preview conflicts for week 202648 skips the canceled entry
        when(semesterRepo.findById(10L)).thenReturn(Optional.of(semester));
        when(entryRepo.findTimeOverlaps(10L, 1, LocalTime.of(10, 0), LocalTime.of(12, 0)))
                .thenReturn(List.of(existing));
        when(cancellationRepo.findByEntryIdInOrderByIsoYearWeekAsc(List.of(40L)))
                .thenReturn(List.of(savedCancellation));

        TimetableEntryRequest candidate = new TimetableEntryRequest(
                1, LocalTime.of(10, 0), LocalTime.of(12, 0), 1, 14, WeekParity.ALL,
                "EC105", "Prof", "Note", true, "15");
        List<TimetableEntryResponse> conflictsInCanceledWeek =
                timetableService.previewConflicts(10L, 1L, candidate, null, 202648);
        assertTrue(conflictsInCanceledWeek.isEmpty());

        List<TimetableEntryResponse> conflictsInActiveWeek =
                timetableService.previewConflicts(10L, 1L, candidate, null, 202649);
        assertEquals(1, conflictsInActiveWeek.size());
    }
}
