package com.codelevel.module.gamification.http.rest.dto;

import java.util.UUID;

public record CheckAchievementsRequest(UUID userId, String triggerType, Long currentValue) {}
