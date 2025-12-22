package com.codelevel.module.identity.http.rest.dto;

public record UserPutRequest(
    String username,
    String email,
    String password
) {
}
