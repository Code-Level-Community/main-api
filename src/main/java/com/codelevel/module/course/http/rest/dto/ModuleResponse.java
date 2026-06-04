package com.codelevel.module.course.http.rest.dto;

import java.time.LocalDateTime;

public record ModuleResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        Long orderPosition,
        LocalDateTime createdAt
) {}
