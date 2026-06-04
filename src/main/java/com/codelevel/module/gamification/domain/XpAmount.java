package com.codelevel.module.gamification.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record XpAmount(long value) {
    public XpAmount {
        if (value <= 0) throw new BusinessRuleException("XP amount must be greater than zero");
    }
}
