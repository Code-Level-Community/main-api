package com.codelevel.module.course_requests.http.rest.dto;

public record CourseRequestCreateRequest(
        String title,
        String description,
        Long categoryId
) {}
