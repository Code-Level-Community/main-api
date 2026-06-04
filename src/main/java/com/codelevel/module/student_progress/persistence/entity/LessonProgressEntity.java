package com.codelevel.module.student_progress.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Table(
    name = "CL_LESSON_PROGRESS",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_lesson_progress_user_lesson",
        columnNames = {"lp_user_id", "lp_lesson_id"}
    )
)
@Entity
public class LessonProgressEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_lesson_progress_seq", allocationSize = 1)
    private Long id;

    @Column(name = "lp_user_id", nullable = false)
    private UUID userId;

    @Column(name = "lp_lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "lp_completed", nullable = false)
    private boolean completed;

    @Column(name = "lp_watch_time_seconds")
    private Long watchTimeSeconds;

    @Column(name = "lp_video_duration_seconds")
    private Long videoDurationSeconds;

    @Column(name = "lp_completion_percentage")
    private Double completionPercentage;

    @Column(name = "lp_started_at")
    private LocalDateTime startedAt;

    @Column(name = "lp_completed_at")
    private LocalDateTime completedAt;

    @Column(name = "lp_last_watched_at")
    private LocalDateTime lastWatchedAt;

    @PrePersist
    public void prePersist() {
        startedAt = LocalDateTime.now();
        if (watchTimeSeconds == null) watchTimeSeconds = 0L;
        if (completionPercentage == null) completionPercentage = 0.0;
    }

    public static Optional<LessonProgressEntity> findByUserAndLesson(UUID userId, Long lessonId) {
        return find("userId = ?1 and lessonId = ?2", userId, lessonId).firstResultOptional();
    }

    public static List<LessonProgressEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public static List<LessonProgressEntity> findByLessonId(Long lessonId) {
        return find("lessonId", lessonId).list();
    }

    public static long countCompletedByUserId(UUID userId) {
        return count("userId = ?1 and completed = true", userId);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Long getWatchTimeSeconds() { return watchTimeSeconds; }
    public void setWatchTimeSeconds(Long watchTimeSeconds) { this.watchTimeSeconds = watchTimeSeconds; }

    public Long getVideoDurationSeconds() { return videoDurationSeconds; }
    public void setVideoDurationSeconds(Long videoDurationSeconds) { this.videoDurationSeconds = videoDurationSeconds; }

    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getLastWatchedAt() { return lastWatchedAt; }
    public void setLastWatchedAt(LocalDateTime lastWatchedAt) { this.lastWatchedAt = lastWatchedAt; }
}
