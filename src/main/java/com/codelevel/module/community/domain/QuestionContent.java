package com.codelevel.module.community.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record QuestionContent(String value) {

    public QuestionContent {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Question content cannot be blank");
        }
        if (value.trim().length() < 20) {
            throw new BusinessRuleException("Question content must be at least 20 characters");
        }
        if (value.trim().length() > 5000) {
            throw new BusinessRuleException("Question content must be at most 5000 characters");
        }
    }
}
