package com.codelevel.module.student_progress.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnrollmentResponse(
        Long id,
        UUID userId,
        Long courseId,
        LocalDateTime enrolledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Double progressPercentage,
        Long lessonsCompleted,
        Long totalStudyTimeMinutes
) {}
