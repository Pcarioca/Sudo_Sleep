package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.ActivityRequirementRequest;
import com.sudosleep.backend.dto.ActivityRequirementResponse;
import com.sudosleep.backend.dto.UnscheduledRequirementResponse;
import com.sudosleep.backend.entity.ActivityRequirement;
import com.sudosleep.backend.entity.Semester;
import com.sudosleep.backend.entity.Subject;
import com.sudosleep.backend.exception.ResourceNotFoundException;
import com.sudosleep.backend.mapper.ActivityRequirementMapper;
import com.sudosleep.backend.repository.ActivityRequirementRepository;
import com.sudosleep.backend.repository.SemesterRepository;
import com.sudosleep.backend.repository.SubjectRepository;
import com.sudosleep.backend.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ActivityRequirementService {

    private final ActivityRequirementRepository requirements;
    private final SubjectRepository subjects;
    private final SemesterRepository semesters;
    private final TimetableEntryRepository entries;
    private final ActivityRequirementMapper mapper;

    public ActivityRequirementService(ActivityRequirementRepository requirements,
                                      SubjectRepository subjects,
                                      SemesterRepository semesters,
                                      TimetableEntryRepository entries,
                                      ActivityRequirementMapper mapper) {
        this.requirements = requirements;
        this.subjects = subjects;
        this.semesters = semesters;
        this.entries = entries;
        this.mapper = mapper;
    }

    public List<ActivityRequirementResponse> listForSubject(Long subjectId, Long currentUserId) {
        findOwnedSubject(subjectId, currentUserId);
        Set<Long> scheduledIds = new HashSet<>(entries.findScheduledRequirementIdsBySubjectIds(List.of(subjectId)));
        return requirements.findBySubjectIdOrderByIdAsc(subjectId).stream()
                .map(r -> mapper.toResponse(r, scheduledIds.contains(r.getId())))
                .toList();
    }

    /** Requirements of the semester that are not placed in the timetable yet. */
    public List<UnscheduledRequirementResponse> listUnscheduled(Long semesterId, Long currentUserId) {
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", semesterId));
        OwnershipGuard.requireOwner(semester.getUser().getId(), currentUserId);
        return requirements.findUnscheduledBySemesterId(semesterId).stream()
                .map(mapper::toUnscheduledResponse)
                .toList();
    }

    @Transactional
    public ActivityRequirementResponse add(Long subjectId, Long currentUserId, ActivityRequirementRequest request) {
        Subject subject = findOwnedSubject(subjectId, currentUserId);
        String customName = RequirementRules.customNameFor(request);
        ActivityRequirement saved = requirements.save(
                new ActivityRequirement(subject, request.activityType(), customName));
        return mapper.toResponse(saved, false);
    }

    @Transactional
    public ActivityRequirementResponse update(Long id, Long currentUserId, ActivityRequirementRequest request) {
        ActivityRequirement requirement = findOwned(id, currentUserId);
        String customName = RequirementRules.customNameFor(request);
        requirement.setActivityType(request.activityType());
        requirement.setCustomTypeName(customName);
        return mapper.toResponse(requirement, entries.existsByRequirementId(id));
    }

    /** Deletes the requirement and, through the database cascade, its timetable entries. */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        requirements.delete(findOwned(id, currentUserId));
    }

    private Subject findOwnedSubject(Long subjectId, Long currentUserId) {
        Subject subject = subjects.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
        OwnershipGuard.requireOwner(subject.getSemester().getUser().getId(), currentUserId);
        return subject;
    }

    private ActivityRequirement findOwned(Long id, Long currentUserId) {
        ActivityRequirement requirement = requirements.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement", id));
        OwnershipGuard.requireOwner(requirement.getSubject().getSemester().getUser().getId(), currentUserId);
        return requirement;
    }
}
