package com.codelevel.module.course_requests.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseRequestResponse(
        Long id,
        UUID requesterId,
        String title,
        String description,
        Long categoryId,
        String status,
        Long upvotes,
        Long downvotes,
        Double approvalPercentage,
        UUID assignedInstructorId,
        Long createdCourseId,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime completedAt
) {}
