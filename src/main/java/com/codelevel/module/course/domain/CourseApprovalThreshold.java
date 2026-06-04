package com.codelevel.module.course.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record CourseApprovalThreshold(double value) {

    public CourseApprovalThreshold {
        if (value <= 0 || value > 100) {
            throw new BusinessRuleException("Approval threshold must be between 1 and 100");
        }
    }
}
