package com.sudosleep.backend.repository;

import com.sudosleep.backend.entity.AcademicEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AcademicEventRepository extends JpaRepository<AcademicEvent, Long> {

    @Query("""
            select e from AcademicEvent e
            left join fetch e.subject s
            where e.semester.id = :semesterId
            order by e.eventDate asc, e.startTime asc
            """)
    List<AcademicEvent> findAllBySemesterId(@Param("semesterId") Long semesterId);

    @Query("""
            select e from AcademicEvent e
            left join fetch e.subject s
            where e.semester.id = :semesterId
              and (:fromDate is null or e.eventDate >= :fromDate)
              and (:toDate is null or e.eventDate <= :toDate)
            order by e.eventDate asc, e.startTime asc
            """)
    List<AcademicEvent> findBySemesterIdAndDateRange(@Param("semesterId") Long semesterId,
                                                     @Param("fromDate") LocalDate fromDate,
                                                     @Param("toDate") LocalDate toDate);

    @Query("""
            select e from AcademicEvent e
            join fetch e.semester sem
            join fetch sem.user u
            left join fetch e.subject s
            where e.id = :id
            """)
    Optional<AcademicEvent> findDetailedById(@Param("id") Long id);
}
