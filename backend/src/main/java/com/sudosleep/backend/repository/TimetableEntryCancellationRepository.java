package com.sudosleep.backend.repository;

import com.sudosleep.backend.entity.TimetableEntryCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TimetableEntryCancellationRepository extends JpaRepository<TimetableEntryCancellation, Long> {

    List<TimetableEntryCancellation> findByEntryIdOrderByIsoYearWeekAsc(Long entryId);

    List<TimetableEntryCancellation> findByEntryIdInOrderByIsoYearWeekAsc(Collection<Long> entryIds);

    Optional<TimetableEntryCancellation> findByEntryIdAndIsoYearWeek(Long entryId, int isoYearWeek);

    boolean existsByEntryIdAndIsoYearWeek(Long entryId, int isoYearWeek);

    void deleteByEntryIdAndIsoYearWeek(Long entryId, int isoYearWeek);
}
