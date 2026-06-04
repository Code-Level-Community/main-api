package com.codelevel.module.course.http.rest.dto;

public record LessonCreateRequest(
        String title,
        String description,
        String contentType,
        String videoUrl,
        Long videoDurationSeconds,
        String textContent,
        Long orderPosition,
        Long xpReward
) {}
