package com.codelevel.module.student_progress.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "CL_EXERCISE_ATTEMPT")
@Entity
public class ExerciseAttemptEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_exercise_attempt_seq", allocationSize = 1)
    private Long id;

    @Column(name = "ea_user_id", nullable = false)
    private UUID userId;

    @Column(name = "ea_exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "ea_attempt_number", nullable = false)
    private Long attemptNumber;

    @Column(name = "ea_submitted_answer", columnDefinition = "text")
    private String submittedAnswer;

    @Column(name = "ea_correct", nullable = false)
    private boolean correct;

    @Column(name = "ea_xp_earned", nullable = false)
    private Long xpEarned;

    @Column(name = "ea_feedback", columnDefinition = "text")
    private String feedback;

    @Column(name = "ea_submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        submittedAt = LocalDateTime.now();
        if (xpEarned == null) xpEarned = 0L;
    }

    public static List<ExerciseAttemptEntity> findByUserAndExercise(UUID userId, Long exerciseId) {
        return find("userId = ?1 and exerciseId = ?2 order by attemptNumber asc", userId, exerciseId).list();
    }

    public static long countByUserAndExercise(UUID userId, Long exerciseId) {
        return count("userId = ?1 and exerciseId = ?2", userId, exerciseId);
    }

    public static boolean hasCorrectAttempt(UUID userId, Long exerciseId) {
        return count("userId = ?1 and exerciseId = ?2 and correct = true", userId, exerciseId) > 0;
    }

    public static long countFailedByUserAndExercise(UUID userId, Long exerciseId) {
        return count("userId = ?1 and exerciseId = ?2 and correct = false", userId, exerciseId);
    }

    public static List<ExerciseAttemptEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getExerciseId() { return exerciseId; }
    public void setExerciseId(Long exerciseId) { this.exerciseId = exerciseId; }

    public Long getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Long attemptNumber) { this.attemptNumber = attemptNumber; }

    public String getSubmittedAnswer() { return submittedAnswer; }
    public void setSubmittedAnswer(String submittedAnswer) { this.submittedAnswer = submittedAnswer; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public Long getXpEarned() { return xpEarned; }
    public void setXpEarned(Long xpEarned) { this.xpEarned = xpEarned; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
