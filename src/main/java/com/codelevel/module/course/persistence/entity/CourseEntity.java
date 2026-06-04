package com.codelevel.module.course.persistence.entity;

import com.codelevel.module.course.persistence.entity.enums.DifficultyCourse;
import com.codelevel.module.course.persistence.entity.enums.StatusCourse;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Table(name = "CL_COURSE")
@Entity
public class CourseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_course_seq", sequenceName = "cl_course_seq", allocationSize = 1)
    private Long id;

    @Column(name = "co_instructor_id")
    private UUID instructorId;

    @Column(name = "co_title", nullable = false)
    private String title;

    @Column(name = "co_description", columnDefinition = "text", nullable = false)
    private String description;

    @Column(name = "co_thumbnail_url", nullable = false)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "co_difficulty_level", nullable = false)
    private DifficultyCourse difficultyLevel;

    @Column(name = "co_total_duration_minutes")
    private Long totalDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "co_status")
    private StatusCourse status;

    @Column(name = "co_approval_threshold")
    private Double approvalThreshold;

    @Column(name = "co_total_lessons")
    private Long totalLessons;

    @Column(name = "co_total_enrollments")
    private Long totalEnrollments;

    @Column(name = "co_average_rating")
    private Double averageRating;

    @Column(name = "co_published_at")
    private LocalDateTime publishedAt;

    @Column(name = "co_created_at")
    private LocalDateTime createdAt;

    @Column(name = "co_updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (Objects.isNull(status)) status = StatusCourse.DRAFT;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static List<CourseEntity> findByInstructorId(UUID instructorId) {
        return find("instructorId", instructorId).list();
    }

    public static List<CourseEntity> findByStatus(StatusCourse status) {
        return find("status", status).list();
    }

    public static List<CourseEntity> findAllEnabled() {
        return find("status != ?1", StatusCourse.ARCHIVED).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getInstructorId() { return instructorId; }
    public void setInstructorId(UUID instructorId) { this.instructorId = instructorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public DifficultyCourse getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(DifficultyCourse difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public Long getTotalDurationMinutes() { return totalDurationMinutes; }
    public void setTotalDurationMinutes(Long totalDurationMinutes) { this.totalDurationMinutes = totalDurationMinutes; }

    public StatusCourse getStatus() { return status; }
    public void setStatus(StatusCourse status) { this.status = status; }

    public Double getApprovalThreshold() { return approvalThreshold; }
    public void setApprovalThreshold(Double approvalThreshold) { this.approvalThreshold = approvalThreshold; }

    public Long getTotalLessons() { return totalLessons; }
    public void setTotalLessons(Long totalLessons) { this.totalLessons = totalLessons; }

    public Long getTotalEnrollments() { return totalEnrollments; }
    public void setTotalEnrollments(Long totalEnrollments) { this.totalEnrollments = totalEnrollments; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
