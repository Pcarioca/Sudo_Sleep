package com.sudosleep.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    public static final String DEFAULT_REMINDER_OFFSETS = "15,60";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "google_sub", unique = true, length = 255)
    private String googleSub;

    @Column(name = "facebook_id", unique = true, length = 255)
    private String facebookId;

    @Column(name = "has_completed_onboarding", nullable = false)
    private boolean hasCompletedOnboarding = false;

    @Column(name = "default_reminder_offsets_csv", nullable = false, length = 64)
    private String defaultReminderOffsetsCsv = DEFAULT_REMINDER_OFFSETS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
        // required by JPA
    }

    public User(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public User(String email, String displayName, String passwordHash, AuthProvider authProvider) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.authProvider = authProvider;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (authProvider == null) {
            authProvider = AuthProvider.LOCAL;
        }
        if (defaultReminderOffsetsCsv == null) {
            defaultReminderOffsetsCsv = DEFAULT_REMINDER_OFFSETS;
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public boolean isHasCompletedOnboarding() {
        return hasCompletedOnboarding;
    }

    public void setHasCompletedOnboarding(boolean hasCompletedOnboarding) {
        this.hasCompletedOnboarding = hasCompletedOnboarding;
    }

    public String getDefaultReminderOffsetsCsv() {
        return defaultReminderOffsetsCsv;
    }

    public void setDefaultReminderOffsetsCsv(String defaultReminderOffsetsCsv) {
        this.defaultReminderOffsetsCsv = defaultReminderOffsetsCsv;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
