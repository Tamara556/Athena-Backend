package com.athena.badge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_badge", uniqueConstraints =
        @UniqueConstraint(name = "uk_user_badge", columnNames = {"user_id", "badge_id"}))
@Getter
@Setter
@NoArgsConstructor
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "badge_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_user_badge_badge"))
    private Badge badge;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private Instant awardedAt;

    public UserBadge(UUID userId, Badge badge) {
        this.userId = userId;
        this.badge = badge;
    }

    @PrePersist
    void onCreate() {
        this.awardedAt = Instant.now();
    }
}
