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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "activity_requirements",
        indexes = @Index(name = "idx_activity_requirements_subject_id", columnList = "subject_id"))
@Check(constraints = "(activity_type = 'CUSTOM') = (custom_type_name is not null)")
public class ActivityRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private ActivityType activityType;

    /** Only set when {@code activityType == CUSTOM}. */
    @Column(name = "custom_type_name", length = 100)
    private String customTypeName;

    protected ActivityRequirement() {
        // required by JPA
    }

    public ActivityRequirement(Subject subject, ActivityType activityType, String customTypeName) {
        this.subject = subject;
        this.activityType = activityType;
        this.customTypeName = customTypeName;
    }

    public Long getId() {
        return id;
    }

    public Subject getSubject() {
        return subject;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getCustomTypeName() {
        return customTypeName;
    }

    public void setCustomTypeName(String customTypeName) {
        this.customTypeName = customTypeName;
    }
}
