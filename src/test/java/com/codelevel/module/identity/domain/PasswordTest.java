package com.codelevel.module.identity.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordTest {

    @Test
    void shouldCreateWithMinimumLength() {
        var password = new Password("Secret1!");
        assertEquals("Secret1!", password.value());
    }

    @Test
    void shouldCreateWithLongPassword() {
        var password = new Password("mySecurePassword!@#123");
        assertEquals("mySecurePassword!@#123", password.value());
    }

    @Test
    void shouldThrowWhenValueIsNull() {
        var ex = assertThrows(BusinessRuleException.class, () -> new Password(null));
        assertEquals("A senha não pode ser vazia", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldThrowWhenValueIsBlank(String blank) {
        var ex = assertThrows(BusinessRuleException.class, () -> new Password(blank));
        assertEquals("A senha não pode ser vazia", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab", "abc", "abcd", "abcde"})
    void shouldThrowWhenShorterThan8Characters(String short_) {
        var ex = assertThrows(BusinessRuleException.class, () -> new Password(short_));
        assertEquals("A senha deve ter pelo menos 8 caracteres", ex.getMessage());
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertEquals(new Password("Secret1!"), new Password("Secret1!"));
    }

    @Test
    void shouldNotBeEqualWhenDifferentValue() {
        assertNotEquals(new Password("Secret1!"), new Password("Other1!a"));
    }
}
