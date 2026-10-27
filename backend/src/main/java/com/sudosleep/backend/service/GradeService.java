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

    public List<GradeResponse> listForSubject(Long subjectId) {
        requireSubject(subjectId);
        return grades.findBySubjectIdOrderByIdAsc(subjectId).stream().map(mapper::toResponse).toList();
    }

    public GradeSummaryResponse summary(Long subjectId) {
        requireSubject(subjectId);
        List<Grade> subjectGrades = grades.findBySubjectIdOrderByIdAsc(subjectId);
        return new GradeSummaryResponse(
                subjectId,
                subjectGrades.size(),
                GradeMath.arithmeticAverage(subjectGrades),
                GradeMath.weightedAverage(subjectGrades));
    }

    @Transactional
    public GradeResponse add(Long subjectId, GradeRequest request) {
        Subject subject = subjects.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
        Grade saved = grades.save(new Grade(subject, request.score(), weightOf(request)));
        return mapper.toResponse(saved);
    }

    @Transactional
    public GradeResponse update(Long id, GradeRequest request) {
        Grade grade = find(id);
        grade.setScore(request.score());
        grade.setWeight(weightOf(request));
        return mapper.toResponse(grade);
    }

    @Transactional
    public void delete(Long id) {
        grades.delete(find(id));
    }

    private Grade find(Long id) {
        return grades.findById(id).orElseThrow(() -> new ResourceNotFoundException("Grade", id));
    }

    private void requireSubject(Long subjectId) {
        if (!subjects.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject", subjectId);
        }
    }

    private static BigDecimal weightOf(GradeRequest request) {
        return request.weight() != null ? request.weight() : DEFAULT_WEIGHT;
    }
}
