package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.SemesterGpaResponse;
import com.sudosleep.backend.dto.SemesterRequest;
import com.sudosleep.backend.dto.SemesterResponse;
import com.sudosleep.backend.entity.Grade;
import com.sudosleep.backend.entity.Semester;
import com.sudosleep.backend.entity.User;
import com.sudosleep.backend.exception.BadRequestException;
import com.sudosleep.backend.exception.ResourceNotFoundException;
import com.sudosleep.backend.mapper.SemesterMapper;
import com.sudosleep.backend.repository.GradeRepository;
import com.sudosleep.backend.repository.SemesterRepository;
import com.sudosleep.backend.repository.TimetableEntryRepository;
import com.sudosleep.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SemesterService {

    static final int DEFAULT_WEEKS_COUNT = 14;

    private final SemesterRepository semesters;
    private final UserRepository users;
    private final GradeRepository grades;
    private final TimetableEntryRepository entries;
    private final SemesterMapper mapper;
    private final Clock clock;

    public SemesterService(SemesterRepository semesters,
                           UserRepository users,
                           GradeRepository grades,
                           TimetableEntryRepository entries,
                           SemesterMapper mapper,
                           Clock clock) {
        this.semesters = semesters;
        this.users = users;
        this.grades = grades;
        this.entries = entries;
        this.mapper = mapper;
        this.clock = clock;
    }

    public List<SemesterResponse> listForUser(Long userId) {
        if (!users.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        return semesters.findByUserIdOrderByStartDateDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public SemesterResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public SemesterResponse create(Long userId, SemesterRequest request) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        int weeksCount = request.weeksCount() != null ? request.weeksCount() : DEFAULT_WEEKS_COUNT;
        requireMonday(request.startDate());
        Semester semester = semesters.save(
                new Semester(user, request.name().trim(), request.startDate(), weeksCount));
        return toResponse(semester);
    }

    @Transactional
    public SemesterResponse update(Long id, SemesterRequest request) {
        Semester semester = find(id);
        int weeksCount = request.weeksCount() != null ? request.weeksCount() : semester.getWeeksCount();
        requireMonday(request.startDate());
        if (weeksCount < semester.getWeeksCount()
                && entries.existsByRequirementSubjectSemesterIdAndWeekToGreaterThan(id, weeksCount)) {
            throw new BadRequestException("Some timetable entries run past week " + weeksCount
                    + ". Shorten or delete them before reducing the number of weeks.");
        }
        semester.setName(request.name().trim());
        semester.setStartDate(request.startDate());
        semester.setWeeksCount(weeksCount);
        return toResponse(semester);
    }

    /** Deletes the semester; its subjects, grades and timetable are removed by the database. */
    @Transactional
    public void delete(Long id) {
        semesters.delete(find(id));
    }

    /** Semester GPA = average of the weighted averages of the subjects that have grades. */
    public SemesterGpaResponse gpa(Long semesterId) {
        if (!semesters.existsById(semesterId)) {
            throw new ResourceNotFoundException("Semester", semesterId);
        }
        Map<Long, List<Grade>> bySubject = new LinkedHashMap<>();
        for (Grade grade : grades.findBySubjectSemesterId(semesterId)) {
            bySubject.computeIfAbsent(grade.getSubject().getId(), key -> new ArrayList<>()).add(grade);
        }
        Collection<List<Grade>> gradesPerSubject = bySubject.values();
        BigDecimal gpa = GradeMath.gpa(gradesPerSubject);
        return new SemesterGpaResponse(semesterId, gradesPerSubject.size(), gpa);
    }

    private Semester find(Long id) {
        return semesters.findById(id).orElseThrow(() -> new ResourceNotFoundException("Semester", id));
    }

    private SemesterResponse toResponse(Semester semester) {
        int week = ScheduleRules.weekNumber(semester.getStartDate(), LocalDate.now(clock));
        Integer currentWeek = week >= 1 && week <= semester.getWeeksCount() ? week : null;
        return mapper.toResponse(semester, currentWeek);
    }

    private static void requireMonday(LocalDate startDate) {
        if (startDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BadRequestException("startDate must be a Monday (the first day of week 1), got "
                    + startDate + " which is a " + startDate.getDayOfWeek());
        }
    }
}
