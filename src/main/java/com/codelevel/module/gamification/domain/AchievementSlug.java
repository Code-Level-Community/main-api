package com.codelevel.module.gamification.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record AchievementSlug(String value) {
    private static final java.util.regex.Pattern VALID_SLUG = java.util.regex.Pattern.compile("^[a-z0-9-]+$");

    public AchievementSlug {
        if (value == null || value.isBlank())
            throw new BusinessRuleException("Slug cannot be blank");
        if (value.length() < 2 || value.length() > 100)
            throw new BusinessRuleException("Slug must be between 2 and 100 characters");
        if (!VALID_SLUG.matcher(value).matches())
            throw new BusinessRuleException("Slug must contain only lowercase letters, numbers and hyphens");
    }
}
