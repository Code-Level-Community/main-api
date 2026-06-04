package com.codelevel.module.identity.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FullNameTest {

    @Test
    void shouldCreateWithValidName() {
        var name = new FullName("Jo Silva");
        assertEquals("Jo Silva", name.value());
    }

    @Test
    void shouldCreateWithLongFullName() {
        var name = new FullName("Ana Maria dos Santos");
        assertEquals("Ana Maria dos Santos", name.value());
    }

    @Test
    void shouldTrimLeadingAndTrailingWhitespace() {
        var name = new FullName("  Jo Silva  ");
        assertEquals("Jo Silva", name.value());
    }

    @Test
    void shouldThrowWhenValueIsNull() {
        var ex = assertThrows(BusinessRuleException.class, () -> new FullName(null));
        assertEquals("Full name is required", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldThrowWhenValueIsBlank(String blank) {
        var ex = assertThrows(BusinessRuleException.class, () -> new FullName(blank));
        assertEquals("Full name is required", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Jo", "Ana", "Jo S", "X Y"})
    void shouldThrowWhenNameIs4CharactersOrLess(String short_) {
        var ex = assertThrows(BusinessRuleException.class, () -> new FullName(short_));
        assertEquals("Full name must be longer than 4 characters", ex.getMessage());
    }

    @Test
    void shouldThrowWhenNameHasNoSpace() {
        var ex = assertThrows(BusinessRuleException.class, () -> new FullName("JoSilva"));
        assertEquals("Full name must contain at least one space between first and last name", ex.getMessage());
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertEquals(new FullName("Jo Silva"), new FullName("Jo Silva"));
    }

    @Test
    void shouldNotBeEqualWhenDifferentValue() {
        assertNotEquals(new FullName("Jo Silva"), new FullName("Ana Santos"));
    }
}
