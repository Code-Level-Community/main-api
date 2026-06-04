package com.codelevel.module.course.http.rest.dto;

public record CourseCreateRequest(
        String title,
        String description,
        String thumbnailUrl,
        String difficultyLevel
) {}
