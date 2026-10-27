package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.ActivityRequirementRequest;
import com.sudosleep.backend.dto.ActivityRequirementResponse;
import com.sudosleep.backend.dto.SubjectRequest;
import com.sudosleep.backend.dto.SubjectResponse;
import com.sudosleep.backend.dto.SubjectUpdateRequest;
import com.sudosleep.backend.entity.ActivityRequirement;
import com.sudosleep.backend.entity.Grade;
import com.sudosleep.backend.entity.Semester;
import com.sudosleep.backend.entity.Subject;
import com.sudosleep.backend.exception.ConflictException;
import com.sudosleep.backend.exception.ResourceNotFoundException;
import com.sudosleep.backend.mapper.ActivityRequirementMapper;
import com.sudosleep.backend.mapper.SubjectMapper;
import com.sudosleep.backend.repository.ActivityRequirementRepository;
import com.sudosleep.backend.repository.GradeRepository;
import com.sudosleep.backend.repository.SemesterRepository;
import com.sudosleep.backend.repository.SubjectRepository;
import com.sudosleep.backend.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjects;
    private final SemesterRepository semesters;
    private final ActivityRequirementRepository requirements;
    private final GradeRepository grades;
    private final TimetableEntryRepository entries;
    private final SubjectMapper mapper;
    private final ActivityRequirementMapper requirementMapper;

    public SubjectService(SubjectRepository subjects,
                          SemesterRepository semesters,
                          ActivityRequirementRepository requirements,
                          GradeRepository grades,
                          TimetableEntryRepository entries,
                          SubjectMapper mapper,
                          ActivityRequirementMapper requirementMapper) {
        this.subjects = subjects;
        this.semesters = semesters;
        this.requirements = requirements;
        this.grades = grades;
        this.entries = entries;
        this.mapper = mapper;
        this.requirementMapper = requirementMapper;
    }

    public List<SubjectResponse> listForSemester(Long semesterId) {
        if (!semesters.existsById(semesterId)) {
            throw new ResourceNotFoundException("Semester", semesterId);
        }
        return toResponses(subjects.findBySemesterIdOrderByNameAsc(semesterId));
    }

    public SubjectResponse get(Long id) {
        return toResponses(List.of(find(id))).get(0);
    }

    /** Creates a subject together with the activities it requires (lecture, laboratory, ...). */
    @Transactional
    public SubjectResponse create(Long semesterId, SubjectRequest request) {
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", semesterId));
        String name = request.name().trim();
        if (subjects.existsBySemesterIdAndNameIgnoreCase(semesterId, name)) {
            throw new ConflictException("This semester already has a subject named '" + name + "'");
        }
        // validate every requirement before saving anything
        List<ActivityRequirementRequest> wanted = request.requirements() == null ? List.of() : request.requirements();
        for (ActivityRequirementRequest requirement : wanted) {
            RequirementRules.customNameFor(requirement);
        }

        Subject subject = subjects.save(new Subject(semester, name, normalizeColor(request.colorHex())));
        for (ActivityRequirementRequest requirement : wanted) {
            requirements.save(new ActivityRequirement(
                    subject, requirement.activityType(), RequirementRules.customNameFor(requirement)));
        }
        return toResponses(List.of(subject)).get(0);
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectUpdateRequest request) {
        Subject subject = find(id);
        String name = request.name().trim();
        if (subjects.existsBySemesterIdAndNameIgnoreCaseAndIdNot(subject.getSemester().getId(), name, id)) {
            throw new ConflictException("This semester already has a subject named '" + name + "'");
        }
        subject.setName(name);
        subject.setColorHex(normalizeColor(request.colorHex()));
        return toResponses(List.of(subject)).get(0);
    }

    /** Deletes the subject; its requirements, grades and timetable entries are removed by the database. */
    @Transactional
    public void delete(Long id) {
        subjects.delete(find(id));
    }

    private Subject find(Long id) {
        return subjects.findById(id).orElseThrow(() -> new ResourceNotFoundException("Subject", id));
    }

    private static String normalizeColor(String colorHex) {
        return colorHex.toUpperCase(Locale.ROOT);
    }

    /** Builds responses for several subjects with one query each for requirements, entries and grades. */
    private List<SubjectResponse> toResponses(List<Subject> subjectList) {
        if (subjectList.isEmpty()) {
            return List.of();
        }
        List<Long> ids = subjectList.stream().map(Subject::getId).toList();

        Map<Long, List<ActivityRequirement>> requirementsBySubject = requirements.findBySubjectIdIn(ids).stream()
                .sorted(Comparator.comparing(ActivityRequirement::getId))
                .collect(Collectors.groupingBy(r -> r.getSubject().getId()));
        Set<Long> scheduledIds = new HashSet<>(entries.findScheduledRequirementIdsBySubjectIds(ids));
        Map<Long, List<Grade>> gradesBySubject = grades.findBySubjectIdIn(ids).stream()
                .collect(Collectors.groupingBy(g -> g.getSubject().getId()));

        return subjectList.stream().map(subject -> {
            List<ActivityRequirementResponse> requirementResponses =
                    requirementsBySubject.getOrDefault(subject.getId(), List.of()).stream()
                            .map(r -> requirementMapper.toResponse(r, scheduledIds.contains(r.getId())))
                            .toList();
            List<Grade> subjectGrades = gradesBySubject.getOrDefault(subject.getId(), List.of());
            return mapper.toResponse(
                    subject,
                    requirementResponses,
                    subjectGrades.size(),
                    GradeMath.arithmeticAverage(subjectGrades),
                    GradeMath.weightedAverage(subjectGrades));
        }).toList();
    }
}
