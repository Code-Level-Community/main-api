package com.codelevel.module.identity.domain;

import com.codelevel.module.identity.domain.exception.BusinessRuleException;

public record Username(String value) {

    public Username {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Name cannot be empty");
        }
        if (value.length() < 3) {
            throw new BusinessRuleException("Name cannot be less than 3 characters");
        }
    }

}
