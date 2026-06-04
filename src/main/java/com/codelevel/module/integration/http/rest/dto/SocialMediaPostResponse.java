package com.codelevel.module.integration.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SocialMediaPostResponse(
        Long id,
        UUID authorId,
        String platform,
        String postType,
        Long referenceId,
        String content,
        String mediaUrl,
        String status,
        String errorMessage,
        LocalDateTime scheduledAt,
        LocalDateTime postedAt,
        LocalDateTime createdAt
) {}