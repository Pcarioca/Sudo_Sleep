package com.sudosleep.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

/**
 * One recurring slot in the weekly timetable: every {@code dayOfWeek} between
 * {@code startTime} and {@code endTime}, in weeks {@code weekFrom..weekTo}
 * (optionally only odd or even weeks).
 */
@Entity
@Table(name = "timetable_entries", indexes = {
        @Index(name = "idx_timetable_entries_requirement_id", columnList = "requirement_id"),
        @Index(name = "idx_timetable_entries_day_start", columnList = "day_of_week, start_time")
})
@Check(constraints = "day_of_week between 1 and 7 and end_time > start_time"
        + " and week_from >= 1 and week_to >= week_from"
        + " and week_parity in ('ALL','ODD','EVEN')")
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ActivityRequirement requirement;

    /** 1 = Monday ... 7 = Sunday (same as {@link java.time.DayOfWeek#getValue()}). */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "week_from", nullable = false)
    private int weekFrom;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "week_to", nullable = false)
    private int weekTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_parity", nullable = false, length = 4)
    private WeekParity weekParity = WeekParity.ALL;

    @Column(length = 100)
    private String room;

    @Column(length = 150)
    private String teacher;

    protected TimetableEntry() {
        // required by JPA
    }

    public TimetableEntry(ActivityRequirement requirement) {
        this.requirement = requirement;
    }

    public Long getId() {
        return id;
    }

    public ActivityRequirement getRequirement() {
        return requirement;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getWeekFrom() {
        return weekFrom;
    }

    public void setWeekFrom(int weekFrom) {
        this.weekFrom = weekFrom;
    }

    public int getWeekTo() {
        return weekTo;
    }

    public void setWeekTo(int weekTo) {
        this.weekTo = weekTo;
    }

    public WeekParity getWeekParity() {
        return weekParity;
    }

    public void setWeekParity(WeekParity weekParity) {
        this.weekParity = weekParity;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }
}
