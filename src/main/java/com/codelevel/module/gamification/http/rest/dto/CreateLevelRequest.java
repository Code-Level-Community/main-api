package com.codelevel.module.gamification.http.rest.dto;

public record CreateLevelRequest(String name, Long xpRequired, String badgeIconUrl) {}
