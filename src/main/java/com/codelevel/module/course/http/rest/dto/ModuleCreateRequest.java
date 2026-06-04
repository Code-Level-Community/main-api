package com.codelevel.module.course.http.rest.dto;

public record ModuleCreateRequest(
        String title,
        String description,
        Long orderPosition
) {}
