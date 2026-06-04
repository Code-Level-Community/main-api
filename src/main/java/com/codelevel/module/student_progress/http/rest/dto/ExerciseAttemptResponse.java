package com.codelevel.module.student_progress.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExerciseAttemptResponse(
        Long id,
        UUID userId,
        Long exerciseId,
        Long attemptNumber,
        String submittedAnswer,
        boolean correct,
        Long xpEarned,
        String feedback,
        LocalDateTime submittedAt
) {}