package com.codelevel.module.gamification.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Table(
    name = "CL_USER_ACHIEVEMENT",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_achievement",
        columnNames = {"ua_user_id", "ua_achievement_id"}
    )
)
@Entity
public class UserAchievementEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cl_user_achievement_seq")
    @SequenceGenerator(name = "cl_user_achievement_seq", sequenceName = "cl_user_achievement_seq", allocationSize = 1)
    private Long id;

    @Column(name = "ua_user_id", nullable = false)
    private UUID userId;

    @Column(name = "ua_achievement_id", nullable = false)
    private Long achievementId;

    @Column(name = "ua_unlocked_at", nullable = false)
    private LocalDateTime unlockedAt;

    @PrePersist
    public void prePersist() {
        if (unlockedAt == null) unlockedAt = LocalDateTime.now();
    }

    public static List<UserAchievementEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public static Optional<UserAchievementEntity> findByUserAndAchievement(UUID userId, Long achievementId) {
        return find("userId = ?1 and achievementId = ?2", userId, achievementId).firstResultOptional();
    }

    public static long countByUserId(UUID userId) {
        return count("userId", userId);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getAchievementId() { return achievementId; }
    public void setAchievementId(Long achievementId) { this.achievementId = achievementId; }

    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}
