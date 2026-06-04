package com.codelevel.module.reviews_feedbacks.http.rest.dto;

public record LessonFeedbackUpdateRequest(
        boolean isHelpful,
        String comment
) {}
