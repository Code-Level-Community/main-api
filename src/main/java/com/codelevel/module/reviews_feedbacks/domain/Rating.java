package com.codelevel.module.reviews_feedbacks.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record Rating(int score) {
    public Rating {
        if (score < 1 || score > 5) {
            throw new BusinessRuleException("Rating must be between 1 and 5");
        }
    }
}
