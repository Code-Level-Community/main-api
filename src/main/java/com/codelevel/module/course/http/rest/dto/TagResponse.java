package com.codelevel.module.course.http.rest.dto;

import java.time.LocalDateTime;

public record TagResponse(
        Long id,
        String name,
        String slug,
        LocalDateTime createdAt
) {}
