package com.codelevel.module.course.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record CourseTitle(String value) {

    public CourseTitle {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Course title cannot be blank");
        }
        if (value.trim().length() < 3) {
            throw new BusinessRuleException("Course title must be at least 3 characters");
        }
        if (value.trim().length() > 255) {
            throw new BusinessRuleException("Course title cannot exceed 255 characters");
        }
        value = value.trim();
    }
}
