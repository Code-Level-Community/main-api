package com.codelevel.module.gamification.http.rest.dto;

public record CreateAchievementRequest(
        String name,
        String slug,
        String description,
        String iconUrl,
        String triggerType,
        String triggerCriteria,
        Long xpReward
) {}
