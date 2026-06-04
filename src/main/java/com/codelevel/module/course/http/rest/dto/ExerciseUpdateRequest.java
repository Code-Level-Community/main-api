package com.codelevel.module.course.http.rest.dto;

public record ExerciseUpdateRequest(
        String type,
        String title,
        String description,
        String quizData,
        String codeTemplate,
        String testCases,
        Long maxAttempts,
        Long xpReward
) {}
