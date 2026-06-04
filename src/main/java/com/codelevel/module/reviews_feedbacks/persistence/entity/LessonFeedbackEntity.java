package com.codelevel.module.reviews_feedbacks.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "CL_LESSON_FEEDBACK", uniqueConstraints = {
        @UniqueConstraint(name = "uq_lesson_feedback", columnNames = {"lf_user_id", "lf_lesson_id"})
})
public class LessonFeedbackEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cl_lesson_feedback_seq")
    @SequenceGenerator(name = "cl_lesson_feedback_seq", sequenceName = "cl_lesson_feedback_seq", allocationSize = 1)
    private Long id;

    @Column(name = "lf_user_id", nullable = false)
    private UUID userId;

    @Column(name = "lf_lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "lf_is_helpful", nullable = false)
    private boolean isHelpful;

    @Column(name = "lf_comment", length = 1000)
    private String comment;

    @Column(name = "lf_created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public static Optional<LessonFeedbackEntity> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public static Optional<LessonFeedbackEntity> findByUserAndLesson(UUID userId, Long lessonId) {
        return find("userId = ?1 and lessonId = ?2", userId, lessonId).firstResultOptional();
    }

    public static List<LessonFeedbackEntity> findByLessonId(Long lessonId) {
        return find("lessonId", lessonId).list();
    }

    public static List<LessonFeedbackEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public boolean isHelpful() { return isHelpful; }
    public void setHelpful(boolean isHelpful) { this.isHelpful = isHelpful; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
