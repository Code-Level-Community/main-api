package com.codelevel.module.course.http.rest.dto;

public record TagCreateRequest(
        String name,
        String slug
) {}
