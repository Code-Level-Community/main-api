package com.codelevel.module.identity.http.rest.dto;

public record UserSaveRequest(
    String fullName,
    String email,
    String password
) {
}
