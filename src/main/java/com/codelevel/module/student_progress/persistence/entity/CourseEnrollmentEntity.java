package com.codelevel.module.student_progress.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Table(
    name = "CL_COURSE_ENROLLMENT",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_enrollment_user_course",
        columnNames = {"coer_user_id", "coer_course_id"}
    )
)
@Entity
public class CourseEnrollmentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_course_enrollment_seq", allocationSize = 1)
    private Long id;

    @Column(name = "coer_user_id", nullable = false)
    private UUID userId;

    @Column(name = "coer_course_id", nullable = false)
    private Long courseId;

    @Column(name = "coer_enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "coer_started_at")
    private LocalDateTime startedAt;

    @Column(name = "coer_completed_at")
    private LocalDateTime completedAt;

    @Column(name = "coer_progress_percentage", nullable = false)
    private Double progressPercentage;

    @Column(name = "coer_lessons_completed", nullable = false)
    private Long lessonsCompleted;

    @Column(name = "coer_total_study_time_minutes", nullable = false)
    private Long totalStudyTimeMinutes;

    @PrePersist
    public void prePersist() {
        enrolledAt = LocalDateTime.now();
        if (progressPercentage == null) progressPercentage = 0.0;
        if (lessonsCompleted == null) lessonsCompleted = 0L;
        if (totalStudyTimeMinutes == null) totalStudyTimeMinutes = 0L;
    }

    public static Optional<CourseEnrollmentEntity> findByUserAndCourse(UUID userId, Long courseId) {
        return find("userId = ?1 and courseId = ?2", userId, courseId).firstResultOptional();
    }

    public static List<CourseEnrollmentEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public static List<CourseEnrollmentEntity> findByCourseId(Long courseId) {
        return find("courseId", courseId).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }

    public Long getLessonsCompleted() { return lessonsCompleted; }
    public void setLessonsCompleted(Long lessonsCompleted) { this.lessonsCompleted = lessonsCompleted; }

    public Long getTotalStudyTimeMinutes() { return totalStudyTimeMinutes; }
    public void setTotalStudyTimeMinutes(Long totalStudyTimeMinutes) { this.totalStudyTimeMinutes = totalStudyTimeMinutes; }
}