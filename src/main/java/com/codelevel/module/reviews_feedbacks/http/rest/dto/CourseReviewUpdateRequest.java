package com.codelevel.module.reviews_feedbacks.http.rest.dto;

public record CourseReviewUpdateRequest(
        int rating,
        boolean isPositive,
        String comment
) {}
