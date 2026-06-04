package com.codelevel.module.gamification.http.rest.dto;

import java.time.LocalDate;
import java.util.UUID;

public record StreakResponse(
        UUID userId,
        Long currentStreakDays,
        Long longestStreakDays,
        LocalDate lastActivityDate,
        Boolean activeToday,
        Boolean freezeActive
) {}
