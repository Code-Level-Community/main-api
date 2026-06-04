package com.codelevel.module.integration.http.rest.dto;

import java.time.LocalDateTime;

public record CreateSocialMediaPostRequest(
        String platform,
        String postType,
        Long referenceId,
        String content,
        String mediaUrl,
        LocalDateTime scheduledAt
) {}