// java
package com.codelevel.module.identity.http.rest.mapper;

import com.codelevel.module.identity.http.rest.dto.UserResponse;
import com.codelevel.module.identity.persistence.entity.UserEntity;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(UserEntity entity) {
        return UserResponse.safeOf(
            entity.getPublicId(),
            entity.getUsername(),
            entity.getFullName(),
            entity.getEmail(),
            entity.getRolesAsList()
        );
    }
}
