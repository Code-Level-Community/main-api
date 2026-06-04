package com.codelevel.module.certificate.http.rest.client.dto;

import java.util.UUID;

public record UserPublicDto(UUID id, String username, String email, String fullName) {}
