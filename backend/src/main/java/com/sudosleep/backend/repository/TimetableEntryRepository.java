package com.sudosleep.backend.repository;

import com.sudosleep.backend.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    /** Every entry of a semester, with requirement and subject loaded (no lazy loading later). */
    @Query("""
            select e from TimetableEntry e
            join fetch e.requirement r
            join fetch r.subject s
            where s.semester.id = :semesterId
            order by e.dayOfWeek, e.startTime
            """)
    List<TimetableEntry> findAllBySemesterId(@Param("semesterId") Long semesterId);

    @Query("""
            select e from TimetableEntry e
            join fetch e.requirement r
            join fetch r.subject s
            where e.id = :id
            """)
    Optional<TimetableEntry> findDetailedById(@Param("id") Long id);

    /**
     * Entries of the semester on the same day whose time range overlaps
     * {@code [startTime, endTime)}: {@code newStart < existingEnd && newEnd > existingStart}.
     * The week check is done afterwards in {@code ScheduleRules}.
     */
    @Query("""
            select e from TimetableEntry e
            join fetch e.requirement r
            join fetch r.subject s
            where s.semester.id = :semesterId
              and e.dayOfWeek = :dayOfWeek
              and e.startTime < :endTime
              and e.endTime > :startTime
            """)
    List<TimetableEntry> findTimeOverlaps(@Param("semesterId") Long semesterId,
                                          @Param("dayOfWeek") int dayOfWeek,
                                          @Param("startTime") LocalTime startTime,
                                          @Param("endTime") LocalTime endTime);

    /** Ids of the requirements (among the given subjects) that already have at least one entry. */
    @Query("""
            select distinct e.requirement.id from TimetableEntry e
            where e.requirement.subject.id in :subjectIds
            """)
    List<Long> findScheduledRequirementIdsBySubjectIds(@Param("subjectIds") Collection<Long> subjectIds);

    boolean existsByRequirementId(Long requirementId);

    /** True if some entry of the semester runs past week {@code weeks} (used when shrinking a semester). */
    boolean existsByRequirementSubjectSemesterIdAndWeekToGreaterThan(Long semesterId, int weeks);
}
