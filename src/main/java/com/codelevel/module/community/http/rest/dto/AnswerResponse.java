package com.codelevel.module.community.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnswerResponse(
        Long id,
        Long questionId,
        UUID userId,
        String content,
        Long upVotes,
        Long downVotes,
        boolean accepted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
