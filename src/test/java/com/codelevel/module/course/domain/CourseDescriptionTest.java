package com.codelevel.module.course.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CourseDescriptionTest {

    @Test
    void shouldCreateWithValidDescription() {
        var desc = new CourseDescription("Learn Java from the ground up.");
        assertEquals("Learn Java from the ground up.", desc.value());
    }

    @Test
    void shouldThrowWhenValueIsNull() {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseDescription(null));
        assertEquals("Course description cannot be blank", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldThrowWhenValueIsBlank(String blank) {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseDescription(blank));
        assertEquals("Course description cannot be blank", ex.getMessage());
    }

    @Test
    void shouldAcceptDescriptionAtMaximumLength() {
        assertDoesNotThrow(() -> new CourseDescription("A".repeat(5000)));
    }

    @Test
    void shouldThrowWhenDescriptionExceedsMaximumLength() {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseDescription("A".repeat(5001)));
        assertEquals("Course description cannot exceed 5000 characters", ex.getMessage());
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertEquals(new CourseDescription("Same desc"), new CourseDescription("Same desc"));
    }

    @Test
    void shouldNotBeEqualWhenDifferentValue() {
        assertNotEquals(new CourseDescription("Desc A"), new CourseDescription("Desc B"));
    }
}
