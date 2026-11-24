package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.GradeRequest;
import com.sudosleep.backend.dto.GradeResponse;
import com.sudosleep.backend.dto.GradeSummaryResponse;
import com.sudosleep.backend.entity.Grade;
import com.sudosleep.backend.entity.Subject;
import com.sudosleep.backend.exception.ResourceNotFoundException;
import com.sudosleep.backend.mapper.GradeMapper;
import com.sudosleep.backend.repository.GradeRepository;
import com.sudosleep.backend.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GradeService {

    private static final BigDecimal DEFAULT_WEIGHT = new BigDecimal("1.00");

    private final GradeRepository grades;
    private final SubjectRepository subjects;
    private final GradeMapper mapper;

    public GradeService(GradeRepository grades, SubjectRepository subjects, GradeMapper mapper) {
        this.grades = grades;
        this.subjects = subjects;
        this.mapper = mapper;
    }

    public List<GradeResponse> listForSubject(Long subjectId, Long currentUserId) {
        findOwnedSubject(subjectId, currentUserId);
        return grades.findBySubjectIdOrderByIdAsc(subjectId).stream().map(mapper::toResponse).toList();
    }

    public GradeSummaryResponse summary(Long subjectId, Long currentUserId) {
        findOwnedSubject(subjectId, currentUserId);
        List<Grade> subjectGrades = grades.findBySubjectIdOrderByIdAsc(subjectId);
        return new GradeSummaryResponse(
                subjectId,
                subjectGrades.size(),
                GradeMath.arithmeticAverage(subjectGrades),
                GradeMath.weightedAverage(subjectGrades));
    }

    @Transactional
    public GradeResponse add(Long subjectId, Long currentUserId, GradeRequest request) {
        Subject subject = findOwnedSubject(subjectId, currentUserId);
        Grade saved = grades.save(new Grade(subject, request.score(), weightOf(request)));
        return mapper.toResponse(saved);
    }

    @Transactional
    public GradeResponse update(Long id, Long currentUserId, GradeRequest request) {
        Grade grade = findOwned(id, currentUserId);
        grade.setScore(request.score());
        grade.setWeight(weightOf(request));
        return mapper.toResponse(grade);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        grades.delete(findOwned(id, currentUserId));
    }

    private Grade findOwned(Long id, Long currentUserId) {
        Grade grade = grades.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", id));
        OwnershipGuard.requireOwner(grade.getSubject().getSemester().getUser().getId(), currentUserId);
        return grade;
    }

    private Subject findOwnedSubject(Long subjectId, Long currentUserId) {
        Subject subject = subjects.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
        OwnershipGuard.requireOwner(subject.getSemester().getUser().getId(), currentUserId);
        return subject;
    }

    private static BigDecimal weightOf(GradeRequest request) {
        return request.weight() != null ? request.weight() : DEFAULT_WEIGHT;
    }
}
