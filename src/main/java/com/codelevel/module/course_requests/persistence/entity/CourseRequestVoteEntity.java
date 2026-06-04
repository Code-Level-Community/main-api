package com.codelevel.module.course_requests.persistence.entity;

import com.codelevel.module.course_requests.persistence.entity.enums.CourseRequestVoteType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Table(
    name = "CL_COURSE_REQUEST_VOTE",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_course_request_vote_user",
        columnNames = {"creqv_course_request_id", "creqv_user_id"}
    )
)
@Entity
public class CourseRequestVoteEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_course_request_vote_seq", allocationSize = 1)
    private Long id;

    @Column(name = "creqv_course_request_id", nullable = false)
    private Long courseRequestId;

    @Column(name = "creqv_user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "creqv_vote_type", nullable = false)
    private CourseRequestVoteType voteType;

    @Column(name = "creqv_created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public static Optional<CourseRequestVoteEntity> findByUserAndRequest(UUID userId, Long courseRequestId) {
        return find("userId = ?1 and courseRequestId = ?2", userId, courseRequestId).firstResultOptional();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseRequestId() { return courseRequestId; }
    public void setCourseRequestId(Long courseRequestId) { this.courseRequestId = courseRequestId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public CourseRequestVoteType getVoteType() { return voteType; }
    public void setVoteType(CourseRequestVoteType voteType) { this.voteType = voteType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
