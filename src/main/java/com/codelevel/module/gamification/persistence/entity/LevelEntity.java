package com.codelevel.module.gamification.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Table(
    name = "CL_LEVEL",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_level_xp_required",
        columnNames = {"lvl_xp_required"}
    )
)
@Entity
public class LevelEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cl_level_seq")
    @SequenceGenerator(name = "cl_level_seq", sequenceName = "cl_level_seq", allocationSize = 1)
    private Long id;

    @Column(name = "lvl_name", nullable = false)
    private String name;

    @Column(name = "lvl_xp_required", nullable = false)
    private Long xpRequired;

    @Column(name = "lvl_badge_icon_url", columnDefinition = "text")
    private String badgeIconUrl;

    @Column(name = "lvl_created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public static List<LevelEntity> listAllOrdered() {
        return findAll(Sort.ascending("xpRequired")).list();
    }

    public static Optional<LevelEntity> findLevelForXp(long totalXp) {
        return find("xpRequired <= ?1", Sort.descending("xpRequired"), totalXp).firstResultOptional();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getXpRequired() { return xpRequired; }
    public void setXpRequired(Long xpRequired) { this.xpRequired = xpRequired; }

    public String getBadgeIconUrl() { return badgeIconUrl; }
    public void setBadgeIconUrl(String badgeIconUrl) { this.badgeIconUrl = badgeIconUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
