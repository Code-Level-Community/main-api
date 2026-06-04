package com.codelevel.module.course.persistence.entity;

import com.codelevel.module.course.persistence.entity.enums.TypeContentLesson;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "CL_LESSON")
@Entity
public class LessonEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_lesson_seq", sequenceName = "cl_lesson_seq", allocationSize = 1)
    private Long id;

    @Column(name = "l_module_id", nullable = false)
    private Long moduleId;

    @Column(name = "l_title", nullable = false)
    private String title;

    @Column(name = "l_description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "l_content_type")
    private TypeContentLesson contentType;

    @Column(name = "l_video_url", columnDefinition = "text")
    private String videoUrl;

    @Column(name = "l_video_duration_seconds")
    private Long videoDurationSeconds;

    @Column(name = "l_text_content", columnDefinition = "text")
    private String textContent;

    @Column(name = "l_order_position")
    private Long orderPosition;

    @Column(name = "l_xp_reward")
    private Long xpReward;

    @Column(name = "l_has_exercises")
    private boolean hasExercises;

    @Column(name = "l_created_at")
    private LocalDateTime createdAt;

    @Column(name = "l_updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static List<LessonEntity> findByModuleId(Long moduleId) {
        return find("moduleId = ?1 order by orderPosition asc nulls last", moduleId).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long moduleId) { this.moduleId = moduleId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TypeContentLesson getContentType() { return contentType; }
    public void setContentType(TypeContentLesson contentType) { this.contentType = contentType; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public Long getVideoDurationSeconds() { return videoDurationSeconds; }
    public void setVideoDurationSeconds(Long videoDurationSeconds) { this.videoDurationSeconds = videoDurationSeconds; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public Long getOrderPosition() { return orderPosition; }
    public void setOrderPosition(Long orderPosition) { this.orderPosition = orderPosition; }

    public Long getXpReward() { return xpReward; }
    public void setXpReward(Long xpReward) { this.xpReward = xpReward; }

    public boolean isHasExercises() { return hasExercises; }
    public void setHasExercises(boolean hasExercises) { this.hasExercises = hasExercises; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
