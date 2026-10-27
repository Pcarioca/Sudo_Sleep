package com.sudosleep.backend.repository;

import com.sudosleep.backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findBySemesterIdOrderByNameAsc(Long semesterId);

    boolean existsBySemesterIdAndNameIgnoreCase(Long semesterId, String name);

    boolean existsBySemesterIdAndNameIgnoreCaseAndIdNot(Long semesterId, String name, Long id);
}
