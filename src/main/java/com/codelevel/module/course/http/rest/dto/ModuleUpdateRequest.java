package com.codelevel.module.course.http.rest.dto;

public record ModuleUpdateRequest(
        String title,
        String description,
        Long orderPosition
) {}
