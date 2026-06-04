package com.codelevel.module.community.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record AnswerContent(String value) {

    public AnswerContent {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Answer content cannot be blank");
        }
        if (value.trim().length() < 10) {
            throw new BusinessRuleException("Answer content must be at least 10 characters");
        }
        if (value.trim().length() > 5000) {
            throw new BusinessRuleException("Answer content must be at most 5000 characters");
        }
    }
}
