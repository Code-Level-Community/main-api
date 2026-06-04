package com.codelevel.module.gamification.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Table(name = "CL_USER_STREAK")
@Entity
public class UserStreakEntity extends PanacheEntityBase {

    @Id
    @Column(name = "us_user_id", nullable = false)
    private UUID userId;

    @Column(name = "us_current_streak_days", nullable = false)
    private Long currentStreakDays = 0L;

    @Column(name = "us_last_activity_date", nullable = false)
    private LocalDate lastActivityDate;

    @Column(name = "us_longest_streak_days", nullable = false)
    private Long longestStreakDays = 0L;

    @Column(name = "us_freeze_used_at")
    private LocalDate freezeUsedAt;

    @Column(name = "us_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static Optional<UserStreakEntity> findByUserId(UUID userId) {
        return findByIdOptional(userId);
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getCurrentStreakDays() { return currentStreakDays; }
    public void setCurrentStreakDays(Long currentStreakDays) { this.currentStreakDays = currentStreakDays; }

    public LocalDate getLastActivityDate() { return lastActivityDate; }
    public void setLastActivityDate(LocalDate lastActivityDate) { this.lastActivityDate = lastActivityDate; }

    public Long getLongestStreakDays() { return longestStreakDays; }
    public void setLongestStreakDays(Long longestStreakDays) { this.longestStreakDays = longestStreakDays; }

    public LocalDate getFreezeUsedAt() { return freezeUsedAt; }
    public void setFreezeUsedAt(LocalDate freezeUsedAt) { this.freezeUsedAt = freezeUsedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
