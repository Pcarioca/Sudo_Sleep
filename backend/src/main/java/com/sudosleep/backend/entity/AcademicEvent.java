package com.sudosleep.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A date-specific academic event such as an Exam, Midterm, Project Deadline, Quiz, or Custom Event.
 * Optionally linked to a {@link Subject}; standalone events have {@code subject == null}.
 */
@Entity
@Table(name = "academic_events", indexes = {
        @Index(name = "idx_academic_events_semester_id", columnList = "semester_id"),
        @Index(name = "idx_academic_events_subject_id", columnList = "subject_id"),
        @Index(name = "idx_academic_events_semester_date", columnList = "semester_id, event_date")
})
@Check(constraints = "end_time > start_time and color_hex ~ '^#[0-9A-Fa-f]{6}$'"
        + " and event_type in ('EXAM','MIDTERM','PROJECT_DEADLINE','QUIZ','CUSTOM_EVENT')")
public class AcademicEvent {

    public static final String DEFAULT_COLOR_HEX = "#D81B60";
    public static final String DEFAULT_REMINDER_OFFSETS = "15,60,1440";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Subject subject;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AcademicEventType eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = 100)
    private String room;

    @Column(length = 500)
    private String notes;

    @Column(name = "color_hex", nullable = false, columnDefinition = "char(7)")
    private String colorHex = DEFAULT_COLOR_HEX;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @Column(name = "reminder_offsets_csv", nullable = false, length = 64)
    private String reminderOffsetsCsv = DEFAULT_REMINDER_OFFSETS;

    protected AcademicEvent() {
        // required by JPA
    }

    public AcademicEvent(Semester semester) {
        this.semester = semester;
    }

    public Long getId() {
        return id;
    }

    public Semester getSemester() {
        return semester;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AcademicEventType getEventType() {
        return eventType;
    }

    public void setEventType(AcademicEventType eventType) {
        this.eventType = eventType;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
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

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getReminderOffsetsCsv() {
        return reminderOffsetsCsv;
    }

    public void setReminderOffsetsCsv(String reminderOffsetsCsv) {
        this.reminderOffsetsCsv = reminderOffsetsCsv;
    }
}
