package com.sudosleep.backend.repository;

import com.sudosleep.backend.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findBySubjectIdOrderByIdAsc(Long subjectId);

    List<Grade> findBySubjectIdIn(Collection<Long> subjectIds);

    List<Grade> findBySubjectSemesterId(Long semesterId);
}
