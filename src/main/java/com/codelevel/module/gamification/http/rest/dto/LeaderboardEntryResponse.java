package com.codelevel.module.gamification.http.rest.dto;

import java.util.UUID;

public record LeaderboardEntryResponse(UUID userId, Long totalXp, LevelResponse currentLevel) {}
