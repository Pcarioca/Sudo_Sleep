package com.sudosleep.backend.repository;

import com.sudosleep.backend.entity.ActivityRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ActivityRequirementRepository extends JpaRepository<ActivityRequirement, Long> {

    List<ActivityRequirement> findBySubjectIdOrderByIdAsc(Long subjectId);

    List<ActivityRequirement> findBySubjectIdIn(Collection<Long> subjectIds);

    /** Requirements of a semester that have no timetable entry yet (the "unscheduled" banner). */
    @Query("""
            select r from ActivityRequirement r
            join fetch r.subject s
            where s.semester.id = :semesterId
              and not exists (select e.id from TimetableEntry e where e.requirement = r)
            order by s.name, r.id
            """)
    List<ActivityRequirement> findUnscheduledBySemesterId(@Param("semesterId") Long semesterId);
}
