package com.codelevel.module.reviews_feedbacks.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record ReviewComment(String text) {
    public ReviewComment {
        if (text != null && text.length() > 1000) {
            throw new BusinessRuleException("Comment cannot exceed 1000 characters");
        }
    }
}
