package com.codelevel.module.identity.domain;

import java.util.Objects;
import com.codelevel.shared.exception.BusinessRuleException;

public record FullName(String value) {

    public FullName {
        if (Objects.isNull(value) || value.isBlank()) {
            throw new BusinessRuleException("Full name is required");
        }

        String trimmed = value.trim();

        if (trimmed.length() <= 4) {
            throw new BusinessRuleException("Full name must be longer than 4 characters");
        }

        if (!trimmed.contains(" ")) {
            throw new BusinessRuleException("Full name must contain at least one space between first and last name");
        }

        value = trimmed;
    }

}
