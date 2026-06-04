package com.codelevel.module.integration.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record PostContent(String value) {

    private static final int MAX_LENGTH = 2200;

    public PostContent {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Post content cannot be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessRuleException("Post content cannot exceed " + MAX_LENGTH + " characters");
        }
    }
}