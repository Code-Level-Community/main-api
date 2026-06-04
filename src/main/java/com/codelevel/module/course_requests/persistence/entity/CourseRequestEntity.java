package com.codelevel.module.course_requests.persistence.entity;

import com.codelevel.module.course_requests.persistence.entity.enums.StatusCourseRequest;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Table(name = "CL_COURSE_REQUEST")
@Entity
public class CourseRequestEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_course_request_seq", allocationSize = 1)
    private Long id;

    @Column(name = "creq_requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "creq_title", nullable = false)
    private String title;

    @Column(name = "creq_description", columnDefinition = "text", nullable = false)
    private String description;

    @Column(name = "creq_category")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "creq_status", nullable = false)
    private StatusCourseRequest status;

    @Column(name = "creq_upvotes", nullable = false)
    private Long upvotes;

    @Column(name = "creq_downvotes", nullable = false)
    private Long downvotes;

    @Column(name = "creq_approval_percentage", nullable = false)
    private Double approvalPercentage;

    @Column(name = "creq_assigned_instructor_id")
    private UUID assignedInstructorId;

    @Column(name = "creq_created_course_id")
    private Long createdCourseId;

    @Column(name = "creq_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "creq_approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "creq_completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (Objects.isNull(status)) status = StatusCourseRequest.PENDING;
        if (Objects.isNull(upvotes)) upvotes = 0L;
        if (Objects.isNull(downvotes)) downvotes = 0L;
        if (Objects.isNull(approvalPercentage)) approvalPercentage = 0.0;
    }

    public static List<CourseRequestEntity> findByStatus(StatusCourseRequest status) {
        return find("status", status).list();
    }

    public static List<CourseRequestEntity> findByRequesterId(UUID requesterId) {
        return find("requesterId", requesterId).list();
    }

    public static List<CourseRequestEntity> findPendingByCategory(Long categoryId) {
        return find("status = ?1 and categoryId = ?2", StatusCourseRequest.PENDING, categoryId).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getRequesterId() { return requesterId; }
    public void setRequesterId(UUID requesterId) { this.requesterId = requesterId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public StatusCourseRequest getStatus() { return status; }
    public void setStatus(StatusCourseRequest status) { this.status = status; }

    public Long getUpvotes() { return upvotes; }
    public void setUpvotes(Long upvotes) { this.upvotes = upvotes; }

    public Long getDownvotes() { return downvotes; }
    public void setDownvotes(Long downvotes) { this.downvotes = downvotes; }

    public Double getApprovalPercentage() { return approvalPercentage; }
    public void setApprovalPercentage(Double approvalPercentage) { this.approvalPercentage = approvalPercentage; }

    public UUID getAssignedInstructorId() { return assignedInstructorId; }
    public void setAssignedInstructorId(UUID assignedInstructorId) { this.assignedInstructorId = assignedInstructorId; }

    public Long getCreatedCourseId() { return createdCourseId; }
    public void setCreatedCourseId(Long createdCourseId) { this.createdCourseId = createdCourseId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
