package com.athena.user.entity;

import com.athena.user.constants.UserConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
public class UserSettings {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String availability;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(nullable = false, length = 20)
    private String style;

    @Column(nullable = false, length = 20)
    private String tone;

    @Column(name = "motivational_messages", nullable = false)
    private boolean motivational;

    @Column(name = "weekly_reflection", nullable = false)
    private boolean reflection;

    @Column(name = "adaptive_recommendations", nullable = false)
    private boolean adaptive;

    @Column(name = "daily_reminder", nullable = false)
    private boolean dailyReminder;

    @Column(name = "weekly_summary", nullable = false)
    private boolean weeklySummary;

    @Column(name = "interview_reminders", nullable = false)
    private boolean interviewReminders;

    @Column(name = "milestone_celebrations", nullable = false)
    private boolean milestones;

    @Column(name = "personalize_history", nullable = false)
    private boolean personalize;

    @Column(name = "share_anonymous", nullable = false)
    private boolean shareAnon;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserSettings(UUID userId) {
        this.userId = userId;
        this.availability = UserConstants.DEFAULT_AVAILABILITY;
        this.difficulty = UserConstants.DEFAULT_DIFFICULTY;
        this.style = UserConstants.DEFAULT_STYLE;
        this.tone = UserConstants.DEFAULT_TONE;
        this.motivational = true;
        this.reflection = true;
        this.adaptive = true;
        this.dailyReminder = true;
        this.weeklySummary = true;
        this.interviewReminders = false;
        this.milestones = true;
        this.personalize = true;
        this.shareAnon = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
