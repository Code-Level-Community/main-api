package com.codelevel.module.identity.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordTest {

    @Test
    void shouldCreateWithMinimumLength() {
        var password = new Password("abc123");
        assertEquals("abc123", password.value());
    }

    @Test
    void shouldCreateWithLongPassword() {
        var password = new Password("mySecurePassword!@#123");
        assertEquals("mySecurePassword!@#123", password.value());
    }

    @Test
    void shouldThrowWhenValueIsNull() {
        var ex = assertThrows(BusinessRuleException.class, () -> new Password(null));
        assertEquals("Password cannot be empty", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldThrowWhenValueIsBlank(String blank) {
        var ex = assertThrows(BusinessRuleException.class, () -> new Password(blank));
        assertEquals("Password cannot be empty", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab", "abc", "abcd", "abcde"})
    void shouldThrowWhenShorterThan6Characters(String short_) {
        var ex = assertThrows(BusinessRuleException.class, () -> new Password(short_));
        assertEquals("Password cannot be less than 6 characters", ex.getMessage());
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertEquals(new Password("secret"), new Password("secret"));
    }

    @Test
    void shouldNotBeEqualWhenDifferentValue() {
        assertNotEquals(new Password("secret"), new Password("other1"));
    }
}
