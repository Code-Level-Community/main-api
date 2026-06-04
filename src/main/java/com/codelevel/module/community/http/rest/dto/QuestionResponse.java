package com.codelevel.module.community.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuestionResponse(
        Long id,
        UUID userId,
        Long courseId,
        String title,
        String content,
        Long upVotes,
        Long downVotes,
        Long answersCount,
        boolean hasAcceptedAnswer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
