package com.codelevel.module.identity.http.rest.dto;

import java.util.UUID;

public record UserSavedResponse(
    UUID id,
    String username,
    String email
) {
}
