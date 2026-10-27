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
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "semesters", indexes = @Index(name = "idx_semesters_user_id", columnList = "user_id"))
@Check(constraints = "weeks_count > 0")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    /** Monday of week 1. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "weeks_count", nullable = false)
    private int weeksCount;

    protected Semester() {
        // required by JPA
    }

    public Semester(User user, String name, LocalDate startDate, int weeksCount) {
        this.user = user;
        this.name = name;
        this.startDate = startDate;
        this.weeksCount = weeksCount;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public int getWeeksCount() {
        return weeksCount;
    }

    public void setWeeksCount(int weeksCount) {
        this.weeksCount = weeksCount;
    }
}
