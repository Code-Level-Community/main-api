package com.codelevel.module.course_requests.persistence.resource;

import com.codelevel.module.course_requests.domain.CourseRequestDescription;
import com.codelevel.module.course_requests.domain.CourseRequestTitle;
import com.codelevel.module.course_requests.http.rest.dto.CourseRequestCreateRequest;
import com.codelevel.module.course_requests.persistence.entity.CourseRequestEntity;
import com.codelevel.module.course_requests.persistence.entity.CourseRequestVoteEntity;
import com.codelevel.module.course_requests.persistence.entity.enums.CourseRequestVoteType;
import com.codelevel.module.course_requests.persistence.entity.enums.StatusCourseRequest;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CourseRequestService implements CreateUpdate {

    private static final int MIN_VOTES_FOR_APPROVAL = 20;
    private static final double APPROVAL_THRESHOLD = 70.0;

    @Transactional
    public CourseRequestEntity create(CourseRequestCreateRequest request, UUID requesterId) {
        var title = new CourseRequestTitle(request.title());
        var description = new CourseRequestDescription(request.description());

        CourseRequestEntity entity = new CourseRequestEntity();
        entity.setRequesterId(requesterId);
        entity.setTitle(title.value());
        entity.setDescription(description.value());
        entity.setCategoryId(request.categoryId());
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public CourseRequestEntity vote(Long requestId, UUID userId, CourseRequestVoteType voteType) {
        CourseRequestEntity request = getById(requestId);

        if (request.getStatus() != StatusCourseRequest.PENDING) {
            throw new BusinessRuleException("Voting is only available for PENDING requests");
        }

        CourseRequestVoteEntity.findByUserAndRequest(userId, requestId).ifPresent(v -> {
            throw new ResourceAlreadyExists("You have already voted on this request");
        });

        CourseRequestVoteEntity vote = new CourseRequestVoteEntity();
        vote.setCourseRequestId(requestId);
        vote.setUserId(userId);
        vote.setVoteType(voteType);
        saveOrUpdate(vote);

        if (voteType == CourseRequestVoteType.UPVOTE) {
            request.setUpvotes(request.getUpvotes() + 1);
        } else {
            request.setDownvotes(request.getDownvotes() + 1);
        }

        long total = request.getUpvotes() + request.getDownvotes();
        double approvalRate = total > 0 ? ((double) request.getUpvotes() / total) * 100 : 0.0;
        request.setApprovalPercentage(approvalRate);

        checkAutoApproval(request, total, approvalRate);
        saveOrUpdate(request);
        return request;
    }

    @Transactional
    public CourseRequestEntity reject(Long requestId) {
        CourseRequestEntity request = getById(requestId);

        if (request.getStatus() != StatusCourseRequest.PENDING && request.getStatus() != StatusCourseRequest.APPROVED) {
            throw new BusinessRuleException("Only PENDING or APPROVED requests can be rejected");
        }

        request.setStatus(StatusCourseRequest.REJECTED);
        saveOrUpdate(request);
        return request;
    }

    @Transactional
    public CourseRequestEntity accept(Long requestId, UUID instructorId) {
        CourseRequestEntity request = getById(requestId);

        if (request.getStatus() != StatusCourseRequest.APPROVED) {
            throw new BusinessRuleException("Only APPROVED requests can be accepted by an instructor");
        }

        request.setStatus(StatusCourseRequest.IN_PROGRESS);
        request.setAssignedInstructorId(instructorId);
        saveOrUpdate(request);
        return request;
    }

    @Transactional
    public CourseRequestEntity giveUp(Long requestId, UUID instructorId) {
        CourseRequestEntity request = getById(requestId);

        if (request.getStatus() != StatusCourseRequest.IN_PROGRESS) {
            throw new BusinessRuleException("Only IN_PROGRESS requests can be returned");
        }

        if (!instructorId.equals(request.getAssignedInstructorId())) {
            throw new BusinessRuleException("Only the assigned instructor can return this request");
        }

        request.setStatus(StatusCourseRequest.APPROVED);
        request.setAssignedInstructorId(null);
        saveOrUpdate(request);
        return request;
    }

    @Transactional
    public CourseRequestEntity complete(Long requestId, UUID instructorId, Long createdCourseId) {
        CourseRequestEntity request = getById(requestId);

        if (request.getStatus() != StatusCourseRequest.IN_PROGRESS) {
            throw new BusinessRuleException("Only IN_PROGRESS requests can be completed");
        }

        if (!instructorId.equals(request.getAssignedInstructorId())) {
            throw new BusinessRuleException("Only the assigned instructor can complete this request");
        }

        request.setStatus(StatusCourseRequest.COMPLETED);
        request.setCreatedCourseId(createdCourseId);
        request.setCompletedAt(LocalDateTime.now());
        saveOrUpdate(request);
        return request;
    }

    public CourseRequestEntity getById(Long id) {
        return CourseRequestEntity.<CourseRequestEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Course request not found"));
    }

    public List<CourseRequestEntity> listAll() {
        return CourseRequestEntity.listAll();
    }

    public List<CourseRequestEntity> listByStatus(StatusCourseRequest status) {
        return CourseRequestEntity.findByStatus(status);
    }

    private void checkAutoApproval(CourseRequestEntity request, long total, double approvalRate) {
        if (total >= MIN_VOTES_FOR_APPROVAL && approvalRate >= APPROVAL_THRESHOLD) {
            request.setStatus(StatusCourseRequest.APPROVED);
            request.setApprovedAt(LocalDateTime.now());
        }
    }
}
