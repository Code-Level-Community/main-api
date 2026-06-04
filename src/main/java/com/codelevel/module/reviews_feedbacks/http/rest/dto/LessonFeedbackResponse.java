package com.codelevel.module.reviews_feedbacks.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LessonFeedbackResponse(
        Long id,
        UUID userId,
        Long lessonId,
        boolean isHelpful,
        String comment,
        LocalDateTime createdAt
) {}
