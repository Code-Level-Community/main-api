package com.codelevel.module.course_requests.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record CourseRequestTitle(String value) {

    public CourseRequestTitle {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Course request title cannot be blank");
        }
        if (value.trim().length() < 3) {
            throw new BusinessRuleException("Course request title must be at least 3 characters");
        }
        if (value.trim().length() > 255) {
            throw new BusinessRuleException("Course request title cannot exceed 255 characters");
        }
        value = value.trim();
    }
}
