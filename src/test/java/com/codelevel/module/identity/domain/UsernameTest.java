package com.codelevel.module.identity.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UsernameTest {

    @Test
    void shouldCreateUsernameWithMinimumLength() {
        var username = new Username("abc");
        assertEquals("abc", username.value());
    }

    @Test
    void shouldCreateUsernameWithLongValue() {
        var username = new Username("lucas_fernandes_dev");
        assertEquals("lucas_fernandes_dev", username.value());
    }

    @Test
    void shouldThrowWhenValueIsNull() {
        var ex = assertThrows(BusinessRuleException.class, () -> new Username(null));
        assertEquals("Name cannot be empty", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void shouldThrowWhenValueIsBlank(String blank) {
        var ex = assertThrows(BusinessRuleException.class, () -> new Username(blank));
        assertEquals("Name cannot be empty", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab"})
    void shouldThrowWhenValueShorterThan3Characters(String short_) {
        var ex = assertThrows(BusinessRuleException.class, () -> new Username(short_));
        assertEquals("Name cannot be less than 3 characters", ex.getMessage());
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertEquals(new Username("lucas"), new Username("lucas"));
    }

    @Test
    void shouldNotBeEqualWhenDifferentValue() {
        assertNotEquals(new Username("lucas"), new Username("ana"));
    }
}
