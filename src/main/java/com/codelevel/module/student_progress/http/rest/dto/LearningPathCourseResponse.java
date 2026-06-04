package com.codelevel.module.student_progress.http.rest.dto;

import java.time.LocalDateTime;

public record LearningPathCourseResponse(
        Long id,
        Long learningPathId,
        Long courseId,
        Long orderPosition,
        String learningObjectives,
        LocalDateTime addedAt
) {}