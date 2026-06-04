package com.codelevel.module.student_progress.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LessonProgressResponse(
        Long id,
        UUID userId,
        Long lessonId,
        boolean completed,
        Long watchTimeSeconds,
        Long videoDurationSeconds,
        Double completionPercentage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime lastWatchedAt
) {}