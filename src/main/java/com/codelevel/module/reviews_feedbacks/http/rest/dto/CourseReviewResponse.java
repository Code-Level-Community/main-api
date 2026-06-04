package com.codelevel.module.reviews_feedbacks.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseReviewResponse(
        Long id,
        UUID userId,
        Long courseId,
        int rating,
        boolean isPositive,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
