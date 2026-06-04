package com.codelevel.module.gamification.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record StreakDays(long value) {
    public StreakDays {
        if (value < 0) throw new BusinessRuleException("Streak days cannot be negative");
    }
}
