package com.codelevel.module.gamification.http.rest.dto;

import java.time.LocalDateTime;

public record AchievementResponse(
        Long id,
        String name,
        String slug,
        String description,
        String iconUrl,
        String triggerType,
        String triggerCriteria,
        Long xpReward,
        LocalDateTime createdAt
) {}
