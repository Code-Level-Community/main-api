package com.codelevel.module.community.http.rest.dto;

public record QuestionCreateRequest(Long courseId, String title, String content) {}
