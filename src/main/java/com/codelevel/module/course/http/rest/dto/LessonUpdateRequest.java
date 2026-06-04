package com.codelevel.module.course.http.rest.dto;

public record LessonUpdateRequest(
        String title,
        String description,
        String contentType,
        String videoUrl,
        Long videoDurationSeconds,
        String textContent,
        Long orderPosition,
        Long xpReward
) {}
