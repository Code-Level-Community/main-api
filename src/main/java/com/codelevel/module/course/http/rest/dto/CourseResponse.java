package com.codelevel.module.course.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseResponse(
        Long id,
        UUID instructorId,
        String title,
        String description,
        String thumbnailUrl,
        String difficultyLevel,
        String status,
        Long totalDurationMinutes,
        Long totalLessons,
        Long totalEnrollments,
        Double averageRating,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {}
