package com.codelevel.module.course.http.rest.dto;

public record CourseUpdateRequest(
        String title,
        String description,
        String thumbnailUrl,
        String difficultyLevel
) {}
