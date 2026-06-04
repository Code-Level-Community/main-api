package com.codelevel.module.community.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTitleTest {

    @Test
    void shouldCreateWithValidTitle() {
        var title = new QuestionTitle("Como usar Spring Boot?");
        assertEquals("Como usar Spring Boot?", title.value());
    }

    @Test
    void shouldCreateWithMinimumLength() {
        var title = new QuestionTitle("Abcde");
        assertEquals("Abcde", title.value());
    }

    @Test
    void shouldThrowWhenNull() {
        assertThrows(BusinessRuleException.class, () -> new QuestionTitle(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldThrowWhenBlank(String blank) {
        assertThrows(BusinessRuleException.class, () -> new QuestionTitle(blank));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab", "abc", "abcd"})
    void shouldThrowWhenTooShort(String short_) {
        assertThrows(BusinessRuleException.class, () -> new QuestionTitle(short_));
    }

    @Test
    void shouldThrowWhenExceeds255Characters() {
        String tooLong = "A".repeat(256);
        assertThrows(BusinessRuleException.class, () -> new QuestionTitle(tooLong));
    }

    @Test
    void shouldAcceptExactly255Characters() {
        String maxLength = "A".repeat(255);
        assertDoesNotThrow(() -> new QuestionTitle(maxLength));
    }
}