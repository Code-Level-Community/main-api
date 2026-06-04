package com.codelevel.module.reviews_feedbacks.http.rest.dto;

public record CourseReviewCreateRequest(
        Long courseId,
        int rating,
        boolean isPositive,
        String comment
) {}
