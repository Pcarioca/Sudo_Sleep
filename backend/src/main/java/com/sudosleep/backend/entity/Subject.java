package com.sudosleep.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "subjects",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_subjects_semester_name",
                columnNames = {"semester_id", "name"}))
@Check(constraints = "color_hex ~ '^#[0-9A-Fa-f]{6}$'")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Semester semester;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "color_hex", nullable = false, columnDefinition = "char(7)")
    private String colorHex;

    protected Subject() {
        // required by JPA
    }

    public Subject(Semester semester, String name, String colorHex) {
        this.semester = semester;
        this.name = name;
        this.colorHex = colorHex;
    }

    public Long getId() {
        return id;
    }

    public Semester getSemester() {
        return semester;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
}
