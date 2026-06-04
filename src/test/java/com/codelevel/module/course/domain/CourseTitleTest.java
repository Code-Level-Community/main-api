package com.codelevel.module.course.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CourseTitleTest {

    @Test
    void shouldCreateWithValidTitle() {
        var title = new CourseTitle("Java Fundamentals");
        assertEquals("Java Fundamentals", title.value());
    }

    @Test
    void shouldTrimWhitespace() {
        var title = new CourseTitle("  Java  ");
        assertEquals("Java", title.value());
    }

    @Test
    void shouldThrowWhenValueIsNull() {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseTitle(null));
        assertEquals("Course title cannot be blank", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldThrowWhenValueIsBlank(String blank) {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseTitle(blank));
        assertEquals("Course title cannot be blank", ex.getMessage());
    }

    @Test
    void shouldThrowWhenTitleIsTooShort() {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseTitle("AB"));
        assertEquals("Course title must be at least 3 characters", ex.getMessage());
    }

    @Test
    void shouldAcceptTitleAtMinimumLength() {
        assertDoesNotThrow(() -> new CourseTitle("ABC"));
    }

    @Test
    void shouldAcceptTitleAtMaximumLength() {
        assertDoesNotThrow(() -> new CourseTitle("A".repeat(255)));
    }

    @Test
    void shouldThrowWhenTitleExceedsMaximumLength() {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseTitle("A".repeat(256)));
        assertEquals("Course title cannot exceed 255 characters", ex.getMessage());
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertEquals(new CourseTitle("Java"), new CourseTitle("Java"));
    }

    @Test
    void shouldNotBeEqualWhenDifferentValue() {
        assertNotEquals(new CourseTitle("Java"), new CourseTitle("Python"));
    }
}
