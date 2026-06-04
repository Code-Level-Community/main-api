package com.codelevel.module.identity.domain;

import com.codelevel.shared.exception.BusinessRuleException;

import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("E-mail cannot be empty");
        }

        String normalized = value.trim();

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessRuleException("Invalid e-mail format");
        }
    }

}
