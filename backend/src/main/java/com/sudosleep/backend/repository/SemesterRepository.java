package com.sudosleep.backend.repository;

import com.sudosleep.backend.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findByUserIdOrderByStartDateDesc(Long userId);
}
