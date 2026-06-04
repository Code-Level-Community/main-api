package com.codelevel.module.reviews_feedbacks.http.rest.dto;

public record LessonFeedbackCreateRequest(
        Long lessonId,
        boolean isHelpful,
        String comment
) {}
