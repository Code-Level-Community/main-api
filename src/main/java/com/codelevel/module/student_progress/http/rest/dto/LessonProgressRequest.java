package com.codelevel.module.student_progress.http.rest.dto;

public record LessonProgressRequest(
        Long lessonId,
        Long watchTimeSeconds,
        Long videoDurationSeconds
) {}
