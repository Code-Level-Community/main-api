package com.codelevel.module.certificate.http.rest.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnrollmentCompletionDto(
        Long id,
        UUID userId,
        Long courseId,
        LocalDateTime enrolledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Double progressPercentage,
        Long lessonsCompleted,
        Long totalStudyTimeMinutes
) {
    public boolean isCompleted() {
        return completedAt != null;
    }
}
