package com.codelevel.module.course.http.rest.dto;

public record CategoryCreateRequest(
        String name,
        String slug,
        String iconUrl
) {}
