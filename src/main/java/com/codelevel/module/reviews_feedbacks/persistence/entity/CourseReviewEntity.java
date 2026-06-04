package com.codelevel.module.reviews_feedbacks.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "CL_COURSE_REVIEW", uniqueConstraints = {
        @UniqueConstraint(name = "uq_course_review", columnNames = {"cr_user_id", "cr_course_id"})
})
public class CourseReviewEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cl_course_review_seq")
    @SequenceGenerator(name = "cl_course_review_seq", sequenceName = "cl_course_review_seq", allocationSize = 1)
    private Long id;

    @Column(name = "cr_user_id", nullable = false)
    private UUID userId;

    @Column(name = "cr_course_id", nullable = false)
    private Long courseId;

    @Column(name = "cr_rating", nullable = false)
    private int rating;

    @Column(name = "cr_is_positive", nullable = false)
    private boolean isPositive;

    @Column(name = "cr_comment", length = 1000)
    private String comment;

    @Column(name = "cr_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "cr_updated_at", nullable = false)
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

    public static Optional<CourseReviewEntity> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public static Optional<CourseReviewEntity> findByUserAndCourse(UUID userId, Long courseId) {
        return find("userId = ?1 and courseId = ?2", userId, courseId).firstResultOptional();
    }

    public static List<CourseReviewEntity> findByCourseId(Long courseId) {
        return find("courseId", courseId).list();
    }

    public static List<CourseReviewEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public boolean isPositive() { return isPositive; }
    public void setPositive(boolean isPositive) { this.isPositive = isPositive; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
