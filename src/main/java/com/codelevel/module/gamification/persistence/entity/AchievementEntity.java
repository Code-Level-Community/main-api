package com.codelevel.module.gamification.persistence.entity;

import com.codelevel.module.gamification.persistence.entity.enums.TriggerAchievement;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Table(name = "CL_ACHIEVEMENT")
@Entity
public class AchievementEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cl_achievement_seq")
    @SequenceGenerator(name = "cl_achievement_seq", sequenceName = "cl_achievement_seq", allocationSize = 1)
    private Long id;

    @Column(name = "ac_name", nullable = false)
    private String name;

    @Column(name = "ac_slug", unique = true, nullable = false)
    private String slug;

    @Column(name = "ac_description", columnDefinition = "text")
    private String description;

    @Column(name = "ac_icon_url", columnDefinition = "text")
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "ac_trigger_type", nullable = false)
    private TriggerAchievement triggerType;

    @Column(name = "ac_trigger_criteria", nullable = false)
    private String triggerCriteria;

    @Column(name = "ac_xp_reward", nullable = false)
    private Long xpReward = 0L;

    @Column(name = "ac_created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (xpReward == null) xpReward = 0L;
    }

    public static Optional<AchievementEntity> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional();
    }

    public static List<AchievementEntity> findByTriggerType(TriggerAchievement triggerType) {
        return find("triggerType", triggerType).list();
    }

    public static List<AchievementEntity> listAllOrdered() {
        return findAll(io.quarkus.panache.common.Sort.by("name")).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public TriggerAchievement getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerAchievement triggerType) { this.triggerType = triggerType; }

    public String getTriggerCriteria() { return triggerCriteria; }
    public void setTriggerCriteria(String triggerCriteria) { this.triggerCriteria = triggerCriteria; }

    public Long getXpReward() { return xpReward; }
    public void setXpReward(Long xpReward) { this.xpReward = xpReward; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
