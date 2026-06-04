package com.codelevel.module.community.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record QuestionTitle(String value) {

    public QuestionTitle {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Question title cannot be blank");
        }
        if (value.trim().length() < 5) {
            throw new BusinessRuleException("Question title must be at least 5 characters");
        }
        if (value.trim().length() > 255) {
            throw new BusinessRuleException("Question title must be at most 255 characters");
        }
    }
}
