package com.codelevel.module.student_progress.http.rest.dto;

public record ExerciseAttemptRequest(
        Long exerciseId,
        String submittedAnswer,
        boolean correct,
        Long baseXpReward,
        int maxAttempts,
        String feedback
) {}