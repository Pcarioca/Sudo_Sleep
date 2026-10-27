package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.TimetableEntryRequest;
import com.sudosleep.backend.dto.TimetableEntryResponse;
import com.sudosleep.backend.entity.ActivityRequirement;
import com.sudosleep.backend.entity.Semester;
import com.sudosleep.backend.entity.TimetableEntry;
import com.sudosleep.backend.entity.WeekParity;
import com.sudosleep.backend.exception.BadRequestException;
import com.sudosleep.backend.exception.ResourceNotFoundException;
import com.sudosleep.backend.exception.ScheduleConflictException;
import com.sudosleep.backend.mapper.TimetableEntryMapper;
import com.sudosleep.backend.repository.ActivityRequirementRepository;
import com.sudosleep.backend.repository.SemesterRepository;
import com.sudosleep.backend.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TimetableService {

    private final TimetableEntryRepository entries;
    private final ActivityRequirementRepository requirements;
    private final SemesterRepository semesters;
    private final TimetableEntryMapper mapper;

    public TimetableService(TimetableEntryRepository entries,
                            ActivityRequirementRepository requirements,
                            SemesterRepository semesters,
                            TimetableEntryMapper mapper) {
        this.entries = entries;
        this.requirements = requirements;
        this.semesters = semesters;
        this.mapper = mapper;
    }

    /**
     * The timetable of a semester. With {@code week} only the entries that actually take place
     * in that week (range and odd/even parity) are returned.
     */
    public List<TimetableEntryResponse> listForSemester(Long semesterId, Integer week) {
        Semester semester = findSemester(semesterId);
        if (week != null && (week < 1 || week > semester.getWeeksCount())) {
            throw new BadRequestException("week must be between 1 and " + semester.getWeeksCount());
        }
        return entries.findAllBySemesterId(semesterId).stream()
                .filter(e -> week == null
                        || ScheduleRules.occursInWeek(e.getWeekFrom(), e.getWeekTo(), e.getWeekParity(), week))
                .map(mapper::toResponse)
                .toList();
    }

    public TimetableEntryResponse get(Long entryId) {
        return mapper.toResponse(findEntry(entryId));
    }

    /** Which existing entries would a slot collide with? Nothing is saved. */
    public List<TimetableEntryResponse> previewConflicts(Long semesterId,
                                                         TimetableEntryRequest request,
                                                         Long excludeEntryId) {
        Semester semester = findSemester(semesterId);
        Slot slot = toSlot(request, semester);
        return findConflicts(semesterId, slot, excludeEntryId).stream().map(mapper::toResponse).toList();
    }

    /**
     * Places a requirement in the timetable. If the slot collides with other entries a
     * {@link ScheduleConflictException} is thrown, unless {@code force} is true.
     */
    @Transactional
    public TimetableEntryResponse create(Long requirementId, TimetableEntryRequest request, boolean force) {
        ActivityRequirement requirement = requirements.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement", requirementId));
        Semester semester = requirement.getSubject().getSemester();
        Slot slot = toSlot(request, semester);
        rejectConflictsUnlessForced(semester.getId(), slot, null, force);

        TimetableEntry entry = new TimetableEntry(requirement);
        apply(entry, slot, request);
        return mapper.toResponse(entries.save(entry));
    }

    @Transactional
    public TimetableEntryResponse update(Long entryId, TimetableEntryRequest request, boolean force) {
        TimetableEntry entry = findEntry(entryId);
        Semester semester = entry.getRequirement().getSubject().getSemester();
        Slot slot = toSlot(request, semester);
        rejectConflictsUnlessForced(semester.getId(), slot, entryId, force);

        apply(entry, slot, request);
        return mapper.toResponse(entry);
    }

    @Transactional
    public void delete(Long entryId) {
        entries.delete(findEntry(entryId));
    }

    // ---- internals --------------------------------------------------------------------

    private void rejectConflictsUnlessForced(Long semesterId, Slot slot, Long excludeEntryId, boolean force) {
        if (force) {
            return;
        }
        List<TimetableEntry> conflicts = findConflicts(semesterId, slot, excludeEntryId);
        if (!conflicts.isEmpty()) {
            throw new ScheduleConflictException(conflicts.stream().map(mapper::toResponse).toList());
        }
    }

    /** Same day + overlapping times (query) + at least one common week (rules). */
    private List<TimetableEntry> findConflicts(Long semesterId, Slot slot, Long excludeEntryId) {
        return entries.findTimeOverlaps(semesterId, slot.dayOfWeek(), slot.startTime(), slot.endTime()).stream()
                .filter(e -> excludeEntryId == null || !e.getId().equals(excludeEntryId))
                .filter(e -> ScheduleRules.shareAWeek(
                        slot.weekFrom(), slot.weekTo(), slot.weekParity(),
                        e.getWeekFrom(), e.getWeekTo(), e.getWeekParity()))
                .toList();
    }

    private static void apply(TimetableEntry entry, Slot slot, TimetableEntryRequest request) {
        entry.setDayOfWeek(slot.dayOfWeek());
        entry.setStartTime(slot.startTime());
        entry.setEndTime(slot.endTime());
        entry.setWeekFrom(slot.weekFrom());
        entry.setWeekTo(slot.weekTo());
        entry.setWeekParity(slot.weekParity());
        entry.setRoom(blankToNull(request.room()));
        entry.setTeacher(blankToNull(request.teacher()));
    }

    /** Applies defaults (whole semester, all weeks) and checks the business rules. */
    private Slot toSlot(TimetableEntryRequest request, Semester semester) {
        LocalTime start = request.startTime();
        LocalTime end = request.endTime();
        if (!end.isAfter(start)) {
            throw new BadRequestException("endTime must be after startTime");
        }
        if (start.isBefore(ScheduleRules.GRID_START) || end.isAfter(ScheduleRules.GRID_END)) {
            throw new BadRequestException("The timetable runs from " + ScheduleRules.GRID_START
                    + " to " + ScheduleRules.GRID_END);
        }
        if (Duration.between(start, end).compareTo(ScheduleRules.MAX_DURATION) > 0) {
            throw new BadRequestException("An activity can last at most "
                    + ScheduleRules.MAX_DURATION.toHours() + " hours");
        }

        int weekFrom = request.weekFrom() != null ? request.weekFrom() : 1;
        int weekTo = request.weekTo() != null ? request.weekTo() : semester.getWeeksCount();
        WeekParity parity = request.weekParity() != null ? request.weekParity() : WeekParity.ALL;
        if (weekTo < weekFrom) {
            throw new BadRequestException("weekTo must be greater than or equal to weekFrom");
        }
        if (weekTo > semester.getWeeksCount()) {
            throw new BadRequestException("The semester has only " + semester.getWeeksCount() + " weeks");
        }
        if (!ScheduleRules.hasAnyWeek(weekFrom, weekTo, parity)) {
            throw new BadRequestException("No " + parity + " week between week " + weekFrom
                    + " and week " + weekTo + ", so the activity would never take place");
        }
        return new Slot(request.dayOfWeek(), start, end, weekFrom, weekTo, parity);
    }

    private Semester findSemester(Long id) {
        return semesters.findById(id).orElseThrow(() -> new ResourceNotFoundException("Semester", id));
    }

    private TimetableEntry findEntry(Long id) {
        return entries.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Timetable entry", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Slot(int dayOfWeek, LocalTime startTime, LocalTime endTime,
                        int weekFrom, int weekTo, WeekParity weekParity) {
    }
}
