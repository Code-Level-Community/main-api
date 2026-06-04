package com.codelevel.module.gamification.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record XpTransactionResponse(
        Long id,
        UUID userId,
        Long xpAmount,
        String source,
        String description,
        LocalDateTime createdAt
) {}
