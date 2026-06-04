package com.codelevel.module.course.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record CourseDescription(String value) {

    public CourseDescription {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Course description cannot be blank");
        }
        if (value.length() > 5000) {
            throw new BusinessRuleException("Course description cannot exceed 5000 characters");
        }
    }
}
