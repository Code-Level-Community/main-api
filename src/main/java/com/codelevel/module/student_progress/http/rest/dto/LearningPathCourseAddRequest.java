package com.codelevel.module.student_progress.http.rest.dto;

public record LearningPathCourseAddRequest(
        Long courseId,
        Long orderPosition,
        String learningObjectives
) {}