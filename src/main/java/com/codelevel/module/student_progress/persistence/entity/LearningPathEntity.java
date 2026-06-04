package com.codelevel.module.student_progress.persistence.entity;

import com.codelevel.module.student_progress.persistence.entity.enums.LearningTrackDifficultyLevel;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Table(name = "CL_LEARNING_PATH")
@Entity
public class LearningPathEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_learning_path_seq", allocationSize = 1)
    private Long id;

    @Column(name = "lpa_creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "lpa_title", nullable = false)
    private String title;

    @Column(name = "lpa_description", columnDefinition = "text")
    private String description;

    @Column(name = "lpa_thumbnail_url")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "lpa_difficulty_level", nullable = false)
    private LearningTrackDifficultyLevel difficultyLevel;

    @Column(name = "lpa_prerequisite")
    private String prerequisite;

    @Column(name = "lpa_courses_count", nullable = false)
    private Long coursesCount;

    @Column(name = "lpa_total_duration_hours")
    private Long totalDurationHours;

    @Column(name = "lpa_published", nullable = false)
    private boolean published;

    @Column(name = "lpa_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "lpa_updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (coursesCount == null) coursesCount = 0L;
        published = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static Optional<LearningPathEntity> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public static List<LearningPathEntity> findByCreatorId(UUID creatorId) {
        return find("creatorId", creatorId).list();
    }

    public static List<LearningPathEntity> findAllPublished() {
        return find("published", true).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getCreatorId() { return creatorId; }
    public void setCreatorId(UUID creatorId) { this.creatorId = creatorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public LearningTrackDifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(LearningTrackDifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getPrerequisite() { return prerequisite; }
    public void setPrerequisite(String prerequisite) { this.prerequisite = prerequisite; }

    public Long getCoursesCount() { return coursesCount; }
    public void setCoursesCount(Long coursesCount) { this.coursesCount = coursesCount; }

    public Long getTotalDurationHours() { return totalDurationHours; }
    public void setTotalDurationHours(Long totalDurationHours) { this.totalDurationHours = totalDurationHours; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
