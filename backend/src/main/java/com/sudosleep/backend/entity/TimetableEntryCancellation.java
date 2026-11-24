package com.sudosleep.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Records a single-week cancellation ("Canceled this week only") for a recurring timetable entry.
 * {@code isoYearWeek} uses {@code isoWeekBasedYear * 100 + isoWeekNumber} (e.g. {@code 202648}),
 * matching the Android client's {@code computeWeekEpochId()}.
 */
@Entity
@Table(
        name = "timetable_entry_cancellations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_timetable_cancellation_entry_week",
                columnNames = {"entry_id", "iso_year_week"}),
        indexes = @Index(name = "idx_timetable_cancellations_entry_id", columnList = "entry_id"))
public class TimetableEntryCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TimetableEntry entry;

    @Column(name = "iso_year_week", nullable = false)
    private int isoYearWeek;

    protected TimetableEntryCancellation() {
        // required by JPA
    }

    public TimetableEntryCancellation(TimetableEntry entry, int isoYearWeek) {
        this.entry = entry;
        this.isoYearWeek = isoYearWeek;
    }

    public Long getId() {
        return id;
    }

    public TimetableEntry getEntry() {
        return entry;
    }

    public int getIsoYearWeek() {
        return isoYearWeek;
    }
}
