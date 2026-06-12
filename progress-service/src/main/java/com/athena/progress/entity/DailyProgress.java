package com.athena.progress.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "daily_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_progress_user_date", columnNames = {"user_id", "activity_date"}))
@Getter
@Setter
@NoArgsConstructor
public class DailyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate date;

    @Column(name = "tasks_completed", nullable = false)
    private int tasksCompleted;

    @Column(name = "minutes_spent", nullable = false)
    private int minutesSpent;

    public DailyProgress(UUID userId, LocalDate date) {
        this.userId = userId;
        this.date = date;
    }

    public void add(int tasks, int minutes) {
        this.tasksCompleted += tasks;
        this.minutesSpent += minutes;
    }
}
