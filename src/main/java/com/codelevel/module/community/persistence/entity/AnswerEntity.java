package com.codelevel.module.community.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Table(name = "CL_ANSWER")
@Entity
public class AnswerEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_answer_seq", allocationSize = 1)
    private Long id;

    @Column(name = "a_question_id", nullable = false)
    private Long questionId;

    @Column(name = "a_user_id", nullable = false)
    private UUID userId;

    @Column(name = "a_content", columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "a_upvotes", nullable = false)
    private Long upVotes;

    @Column(name = "a_downvotes", nullable = false)
    private Long downVotes;

    @Column(name = "a_is_accepted", nullable = false)
    private boolean accepted;

    @Column(name = "a_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "a_updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (upVotes == null) upVotes = 0L;
        if (downVotes == null) downVotes = 0L;
    }

    public static Optional<AnswerEntity> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public static List<AnswerEntity> findByQuestionId(Long questionId) {
        return find("questionId", questionId).list();
    }

    public static Optional<AnswerEntity> findAcceptedByQuestionId(Long questionId) {
        return find("questionId = ?1 and accepted = true", questionId).firstResultOptional();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getUpVotes() { return upVotes; }
    public void setUpVotes(Long upVotes) { this.upVotes = upVotes; }

    public Long getDownVotes() { return downVotes; }
    public void setDownVotes(Long downVotes) { this.downVotes = downVotes; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
