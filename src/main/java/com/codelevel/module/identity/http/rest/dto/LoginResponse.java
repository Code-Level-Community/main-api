package com.codelevel.module.identity.http.rest.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn
) {
}
