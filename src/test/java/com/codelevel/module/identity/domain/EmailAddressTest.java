package com.codelevel.module.identity.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EmailAddressTest {

    @Test
    void shouldCreateWithValidEmail() {
        var email = new EmailAddress("user@example.com");
        assertEquals("user@example.com", email.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "test.user+tag@sub.domain.org",
        "USER@EXAMPLE.COM",
        "user123@domain.co.uk",
        "  user@example.com  "
    })
    void shouldAcceptValidEmailFormats(String raw) {
        assertDoesNotThrow(() -> new EmailAddress(raw));
    }

    @Test
    void shouldThrowWhenValueIsNull() {
        var ex = assertThrows(BusinessRuleException.class, () -> new EmailAddress(null));
        assertEquals("E-mail cannot be empty", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldThrowWhenValueIsBlank(String blank) {
        var ex = assertThrows(BusinessRuleException.class, () -> new EmailAddress(blank));
        assertEquals("E-mail cannot be empty", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "notanemail",
        "@domain.com",
        "user@",
        "user@domain",
        "user.domain.com",
        "user @example.com"
    })
    void shouldThrowWhenEmailFormatIsInvalid(String invalid) {
        var ex = assertThrows(BusinessRuleException.class, () -> new EmailAddress(invalid));
        assertEquals("Invalid e-mail format", ex.getMessage());
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertEquals(new EmailAddress("user@example.com"), new EmailAddress("user@example.com"));
    }

    @Test
    void shouldNotBeEqualWhenDifferentValue() {
        assertNotEquals(new EmailAddress("a@example.com"), new EmailAddress("b@example.com"));
    }
}
