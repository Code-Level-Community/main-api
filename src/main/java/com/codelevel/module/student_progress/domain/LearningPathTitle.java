package com.codelevel.module.student_progress.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record LearningPathTitle(String value) {

    public LearningPathTitle {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Learning path title cannot be blank");
        }
        value = value.trim();
        if (value.length() < 5) {
            throw new BusinessRuleException("Learning path title must be at least 5 characters");
        }
        if (value.length() > 255) {
            throw new BusinessRuleException("Learning path title must be at most 255 characters");
        }
    }
}
