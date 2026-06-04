package com.codelevel.module.course_requests.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CourseRequestDescriptionTest {

    @Test
    void shouldCreateWithValidDescription() {
        var desc = new CourseRequestDescription("Gostaria de aprender Spring Boot do zero ao avançado.");
        assertNotNull(desc.value());
    }

    @Test
    void shouldTrimDescription() {
        var desc = new CourseRequestDescription("  Descrição com espaços  ");
        assertEquals("Descrição com espaços", desc.value());
    }

    @Test
    void shouldThrowWhenNull() {
        assertThrows(BusinessRuleException.class, () -> new CourseRequestDescription(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldThrowWhenBlank(String blank) {
        assertThrows(BusinessRuleException.class, () -> new CourseRequestDescription(blank));
    }

    @Test
    void shouldThrowWhenExceeds2000Characters() {
        String tooLong = "A".repeat(2001);
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseRequestDescription(tooLong));
        assertEquals("Course request description cannot exceed 2000 characters", ex.getMessage());
    }

    @Test
    void shouldAcceptExactly2000Characters() {
        String maxLength = "A".repeat(2000);
        assertDoesNotThrow(() -> new CourseRequestDescription(maxLength));
    }
}
