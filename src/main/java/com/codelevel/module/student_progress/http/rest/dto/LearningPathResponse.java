package com.codelevel.module.student_progress.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LearningPathResponse(
        Long id,
        UUID creatorId,
        String title,
        String description,
        String thumbnailUrl,
        String difficultyLevel,
        String prerequisite,
        Long coursesCount,
        Long totalDurationHours,
        boolean published,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}