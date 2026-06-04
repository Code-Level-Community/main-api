package com.codelevel.module.course.http.rest.dto;

import java.time.LocalDateTime;

public record LessonResponse(
        Long id,
        Long moduleId,
        String title,
        String description,
        String contentType,
        Long orderPosition,
        Long xpReward,
        boolean hasExercises,
        LocalDateTime createdAt
) {}
