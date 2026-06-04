package com.codelevel.module.gamification.http.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserAchievementResponse(
        Long id,
        UUID userId,
        Long achievementId,
        LocalDateTime unlockedAt
) {}
