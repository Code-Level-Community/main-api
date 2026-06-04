package com.codelevel.module.student_progress.http.rest.dto;

public record LearningPathCreateRequest(
        String title,
        String description,
        String thumbnailUrl,
        String difficultyLevel,
        String prerequisite
) {}