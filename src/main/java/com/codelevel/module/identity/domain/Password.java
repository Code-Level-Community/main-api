package com.codelevel.module.identity.domain;

import com.codelevel.module.identity.domain.exception.BusinessRuleException;
import io.quarkus.elytron.security.common.BcryptUtil;

public record Password(String value) {

    public Password {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Password cannot be empty");
        }
        if (value.length() < 6) {
            throw new BusinessRuleException("Password cannot be less than 6 characters");
        }
    }

    // Método útil para verificar senha durante login
    public boolean matches(String password) {
        return BcryptUtil.matches(password, this.value);
    }

}
