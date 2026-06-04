package com.codelevel.module.course.http.rest.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String iconUrl,
        LocalDateTime createdAt
) {}
