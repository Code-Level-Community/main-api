package com.codelevel.module.community.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Table(name = "CL_QUESTION")
@Entity
public class QuestionEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_question_seq", allocationSize = 1)
    private Long id;

    @Column(name = "q_user_id", nullable = false)
    private UUID userId;

    @Column(name = "q_course_id")
    private Long courseId;

    @Column(name = "q_title", nullable = false)
    private String title;

    @Column(name = "q_content", columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "q_up_votes", nullable = false)
    private Long upVotes;

    @Column(name = "q_down_votes", nullable = false)
    private Long downVotes;

    @Column(name = "q_answers_count", nullable = false)
    private Long answersCount;

    @Column(name = "q_has_accepted_answer", nullable = false)
    private boolean hasAcceptedAnswer;

    @Column(name = "q_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "q_updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (upVotes == null) upVotes = 0L;
        if (downVotes == null) downVotes = 0L;
        if (answersCount == null) answersCount = 0L;
    }

    public static Optional<QuestionEntity> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public static List<QuestionEntity> findByCourseId(Long courseId) {
        return find("courseId", courseId).list();
    }

    public static List<QuestionEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getUpVotes() { return upVotes; }
    public void setUpVotes(Long upVotes) { this.upVotes = upVotes; }

    public Long getDownVotes() { return downVotes; }
    public void setDownVotes(Long downVotes) { this.downVotes = downVotes; }

    public Long getAnswersCount() { return answersCount; }
    public void setAnswersCount(Long answersCount) { this.answersCount = answersCount; }

    public boolean isHasAcceptedAnswer() { return hasAcceptedAnswer; }
    public void setHasAcceptedAnswer(boolean hasAcceptedAnswer) { this.hasAcceptedAnswer = hasAcceptedAnswer; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}