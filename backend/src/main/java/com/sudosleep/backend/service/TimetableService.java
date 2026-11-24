package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.TimetableEntryRequest;
import com.sudosleep.backend.dto.TimetableEntryResponse;
import com.sudosleep.backend.entity.ActivityRequirement;
import com.sudosleep.backend.entity.Semester;
import com.sudosleep.backend.entity.TimetableEntry;
import com.sudosleep.backend.entity.TimetableEntryCancellation;
import com.sudosleep.backend.entity.WeekParity;
import com.sudosleep.backend.exception.BadRequestException;
import com.sudosleep.backend.exception.ResourceNotFoundException;
import com.sudosleep.backend.exception.ScheduleConflictException;
import com.sudosleep.backend.mapper.TimetableEntryMapper;
import com.sudosleep.backend.repository.ActivityRequirementRepository;
import com.sudosleep.backend.repository.SemesterRepository;
import com.sudosleep.backend.repository.TimetableEntryCancellationRepository;
import com.sudosleep.backend.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TimetableService {

    private final TimetableEntryRepository entries;
    private final TimetableEntryCancellationRepository cancellations;
    private final ActivityRequirementRepository requirements;
    private final SemesterRepository semesters;
    private final TimetableEntryMapper mapper;

    public TimetableService(TimetableEntryRepository entries,
                            TimetableEntryCancellationRepository cancellations,
                            ActivityRequirementRepository requirements,
                            SemesterRepository semesters,
                            TimetableEntryMapper mapper) {
        this.entries = entries;
        this.cancellations = cancellations;
        this.requirements = requirements;
        this.semesters = semesters;
        this.mapper = mapper;
    }

    /**
     * The timetable of a semester. With {@code week} only the entries that actually take place
     * in that week (range and odd/even parity) are returned.
     */
    public List<TimetableEntryResponse> listForSemester(Long semesterId, Integer week, Long currentUserId) {
        Semester semester = findOwnedSemester(semesterId, currentUserId);
        if (week != null && (week < 1 || week > semester.getWeeksCount())) {
            throw new BadRequestException("week must be between 1 and " + semester.getWeeksCount());
        }
        List<TimetableEntry> filtered = entries.findAllBySemesterId(semesterId).stream()
                .filter(e -> week == null
                        || ScheduleRules.occursInWeek(e.getWeekFrom(), e.getWeekTo(), e.getWeekParity(), week))
                .toList();
        return toResponses(filtered);
    }

    public TimetableEntryResponse get(Long entryId, Long currentUserId) {
        return toResponse(findOwnedEntry(entryId, currentUserId));
    }

    /** Which existing entries would a slot collide with? Nothing is saved. */
    public List<TimetableEntryResponse> previewConflicts(Long semesterId,
                                                         Long currentUserId,
                                                         TimetableEntryRequest request,
                                                         Long excludeEntryId,
                                                         Integer isoYearWeek) {
        Semester semester = findOwnedSemester(semesterId, currentUserId);
        if (isoYearWeek != null) {
            validateIsoYearWeek(isoYearWeek);
        }
        Slot slot = toSlot(request, semester);
        return toResponses(findConflicts(semesterId, slot, excludeEntryId, isoYearWeek));
    }

    /**
     * Places a requirement in the timetable. If the slot collides with other entries a
     * {@link ScheduleConflictException} is thrown, unless {@code force} is true.
     */
    @Transactional
    public TimetableEntryResponse create(Long requirementId,
                                         Long currentUserId,
                                         TimetableEntryRequest request,
                                         boolean force) {
        ActivityRequirement requirement = requirements.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement", requirementId));
        Semester semester = requirement.getSubject().getSemester();
        OwnershipGuard.requireOwner(semester.getUser().getId(), currentUserId);

        Slot slot = toSlot(request, semester);
        rejectConflictsUnlessForced(semester.getId(), slot, null, force);

        TimetableEntry entry = new TimetableEntry(requirement);
        apply(entry, slot, request);
        return mapper.toResponse(entries.save(entry), List.of());
    }

    @Transactional
    public TimetableEntryResponse update(Long entryId,
                                         Long currentUserId,
                                         TimetableEntryRequest request,
                                         boolean force) {
        TimetableEntry entry = findOwnedEntry(entryId, currentUserId);
        Semester semester = entry.getRequirement().getSubject().getSemester();
        Slot slot = toSlot(request, semester);
        rejectConflictsUnlessForced(semester.getId(), slot, entryId, force);

        apply(entry, slot, request);
        return toResponse(entry);
    }

    @Transactional
    public void delete(Long entryId, Long currentUserId) {
        entries.delete(findOwnedEntry(entryId, currentUserId));
    }

    /** Marks a recurring entry as canceled for a single ISO year-week (e.g. {@code 202648}). Idempotent. */
    @Transactional
    public TimetableEntryResponse cancelForWeek(Long entryId, int isoYearWeek, Long currentUserId) {
        validateIsoYearWeek(isoYearWeek);
        TimetableEntry entry = findOwnedEntry(entryId, currentUserId);
        if (!cancellations.existsByEntryIdAndIsoYearWeek(entryId, isoYearWeek)) {
            cancellations.save(new TimetableEntryCancellation(entry, isoYearWeek));
        }
        return toResponse(entry);
    }

    /** Clears a single-week cancellation for a recurring entry. Idempotent. */
    @Transactional
    public TimetableEntryResponse uncancelForWeek(Long entryId, int isoYearWeek, Long currentUserId) {
        validateIsoYearWeek(isoYearWeek);
        TimetableEntry entry = findOwnedEntry(entryId, currentUserId);
        cancellations.deleteByEntryIdAndIsoYearWeek(entryId, isoYearWeek);
        return toResponse(entry);
    }

    // ---- internals --------------------------------------------------------------------

    private void rejectConflictsUnlessForced(Long semesterId, Slot slot, Long excludeEntryId, boolean force) {
        if (force) {
            return;
        }
        List<TimetableEntry> conflicts = findConflicts(semesterId, slot, excludeEntryId, null);
        if (!conflicts.isEmpty()) {
            throw new ScheduleConflictException(toResponses(conflicts));
        }
    }

    /** Same day + overlapping times (query) + at least one common week (rules), optionally ignoring canceled-week entries. */
    private List<TimetableEntry> findConflicts(Long semesterId,
                                               Slot slot,
                                               Long excludeEntryId,
                                               Integer isoYearWeek) {
        List<TimetableEntry> candidates = entries
                .findTimeOverlaps(semesterId, slot.dayOfWeek(), slot.startTime(), slot.endTime()).stream()
                .filter(e -> excludeEntryId == null || !e.getId().equals(excludeEntryId))
                .filter(e -> ScheduleRules.shareAWeek(
                        slot.weekFrom(), slot.weekTo(), slot.weekParity(),
                        e.getWeekFrom(), e.getWeekTo(), e.getWeekParity()))
                .toList();
        if (isoYearWeek == null || candidates.isEmpty()) {
            return candidates;
        }
        List<Long> candidateIds = candidates.stream().map(TimetableEntry::getId).toList();
        Set<Long> canceledInWeek = cancellations.findByEntryIdInOrderByIsoYearWeekAsc(candidateIds).stream()
                .filter(c -> c.getIsoYearWeek() == isoYearWeek)
                .map(c -> c.getEntry().getId())
                .collect(Collectors.toSet());
        return candidates.stream()
                .filter(e -> !canceledInWeek.contains(e.getId()))
                .toList();
    }

    private TimetableEntryResponse toResponse(TimetableEntry entry) {
        List<Integer> canceledWeeks = cancellations.findByEntryIdOrderByIsoYearWeekAsc(entry.getId()).stream()
                .map(TimetableEntryCancellation::getIsoYearWeek)
                .toList();
        return mapper.toResponse(entry, canceledWeeks);
    }

    private List<TimetableEntryResponse> toResponses(List<TimetableEntry> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> ids = list.stream().map(TimetableEntry::getId).toList();
        Map<Long, List<Integer>> canceledByEntry = cancellations.findByEntryIdInOrderByIsoYearWeekAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        c -> c.getEntry().getId(),
                        Collectors.mapping(TimetableEntryCancellation::getIsoYearWeek, Collectors.toList())));
        return list.stream()
                .map(e -> mapper.toResponse(e, canceledByEntry.getOrDefault(e.getId(), List.of())))
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
        entry.setNotes(blankToNull(request.notes()));
        if (request.notificationsEnabled() != null) {
            entry.setNotificationsEnabled(request.notificationsEnabled());
        }
        if (request.reminderOffsetsCsv() != null) {
            entry.setReminderOffsetsCsv(request.reminderOffsetsCsv().trim());
        }
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

    private Semester findOwnedSemester(Long id, Long currentUserId) {
        Semester semester = semesters.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", id));
        OwnershipGuard.requireOwner(semester.getUser().getId(), currentUserId);
        return semester;
    }

    private TimetableEntry findOwnedEntry(Long id, Long currentUserId) {
        TimetableEntry entry = entries.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry", id));
        OwnershipGuard.requireOwner(entry.getRequirement().getSubject().getSemester().getUser().getId(), currentUserId);
        return entry;
    }

    private static void validateIsoYearWeek(int isoYearWeek) {
        int year = isoYearWeek / 100;
        int week = isoYearWeek % 100;
        if (year < 2000 || year > 2100 || week < 1 || week > 53) {
            throw new BadRequestException("isoYearWeek must be YYYYWW (e.g. 202648), got " + isoYearWeek);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Slot(int dayOfWeek, LocalTime startTime, LocalTime endTime,
                        int weekFrom, int weekTo, WeekParity weekParity) {
    }
}
