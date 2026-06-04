package com.codelevel.module.course_requests.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record CourseRequestDescription(String value) {

    public CourseRequestDescription {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Course request description cannot be blank");
        }
        if (value.trim().length() > 2000) {
            throw new BusinessRuleException("Course request description cannot exceed 2000 characters");
        }
        value = value.trim();
    }
}
