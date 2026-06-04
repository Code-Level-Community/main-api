// java
package com.codelevel.module.identity.http.rest.dto;

import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String fullName,
    String email,
    List<String> roles
) {
    public static UserResponse safeOf(UUID id, String username, String fullName, String email, List<String> roles) {
        return new UserResponse(
            id,
            username == null ? "" : username,
            fullName == null ? "" : fullName,
            email    == null ? "" : email,
            roles    == null ? List.of() : roles
        );
    }
}
