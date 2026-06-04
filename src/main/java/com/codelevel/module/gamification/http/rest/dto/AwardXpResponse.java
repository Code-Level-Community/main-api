package com.codelevel.module.gamification.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AwardXpResponse(
        Long id,
        UUID userId,
        Long xpAmount,
        String source,
        String description,
        LocalDateTime createdAt,
        LevelResponse currentLevel,
        boolean leveledUp
) {}
