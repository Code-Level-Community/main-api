package com.codelevel.module.course_requests.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CourseRequestTitleTest {

    @Test
    void shouldCreateWithValidTitle() {
        var title = new CourseRequestTitle("Java para Iniciantes");
        assertEquals("Java para Iniciantes", title.value());
    }

    @Test
    void shouldTrimTitle() {
        var title = new CourseRequestTitle("  Java para Iniciantes  ");
        assertEquals("Java para Iniciantes", title.value());
    }

    @Test
    void shouldCreateWithMinimumLength() {
        var title = new CourseRequestTitle("Abc");
        assertEquals("Abc", title.value());
    }

    @Test
    void shouldThrowWhenNull() {
        assertThrows(BusinessRuleException.class, () -> new CourseRequestTitle(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldThrowWhenBlank(String blank) {
        assertThrows(BusinessRuleException.class, () -> new CourseRequestTitle(blank));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab"})
    void shouldThrowWhenTooShort(String short_) {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseRequestTitle(short_));
        assertEquals("Course request title must be at least 3 characters", ex.getMessage());
    }

    @Test
    void shouldThrowWhenExceeds255Characters() {
        String tooLong = "A".repeat(256);
        assertThrows(BusinessRuleException.class, () -> new CourseRequestTitle(tooLong));
    }

    @Test
    void shouldAcceptExactly255Characters() {
        String maxLength = "A".repeat(255);
        assertDoesNotThrow(() -> new CourseRequestTitle(maxLength));
    }
}
