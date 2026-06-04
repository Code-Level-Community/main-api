package com.codelevel.module.course.http.rest.dto;

import java.time.LocalDateTime;

public record ExerciseResponse(
        Long id,
        Long lessonId,
        String type,
        String title,
        String description,
        Long maxAttempts,
        Long xpReward,
        LocalDateTime createdAt
) {}
