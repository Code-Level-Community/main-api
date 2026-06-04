package com.codelevel.module.gamification.http.rest.dto;

import java.util.UUID;

public record AwardXpRequest(UUID userId, Long amount, String source, Long sourceId, String description) {}
