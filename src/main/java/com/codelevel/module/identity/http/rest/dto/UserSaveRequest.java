package com.codelevel.module.identity.http.rest.dto;

public record UserSaveRequest(
    String username,
    String email,
    String password
) {
}
