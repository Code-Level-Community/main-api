package com.codelevel.module.identity.domain;

import com.codelevel.shared.exception.BusinessRuleException;

public record Password(String value) {

    public Password {
        if (value == null || value.isBlank())
            throw new BusinessRuleException("A senha não pode ser vazia");
        if (value.length() < 8)
            throw new BusinessRuleException("A senha deve ter pelo menos 8 caracteres");
        if (!value.chars().anyMatch(Character::isUpperCase))
            throw new BusinessRuleException("A senha deve conter pelo menos uma letra maiúscula");
        if (!value.chars().anyMatch(Character::isLowerCase))
            throw new BusinessRuleException("A senha deve conter pelo menos uma letra minúscula");
        if (!value.chars().anyMatch(Character::isDigit))
            throw new BusinessRuleException("A senha deve conter pelo menos um número");
        if (value.chars().noneMatch(c -> "!@#$%^&*()-_=+[]{}|;'\",.<>?/\\".indexOf(c) >= 0))
            throw new BusinessRuleException("A senha deve conter pelo menos um caractere especial");
    }

}
