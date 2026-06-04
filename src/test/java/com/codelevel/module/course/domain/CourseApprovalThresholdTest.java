package com.codelevel.module.course.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CourseApprovalThresholdTest {

    @Test
    void shouldCreateWithValidThreshold() {
        var threshold = new CourseApprovalThreshold(70.0);
        assertEquals(70.0, threshold.value());
    }

    @Test
    void shouldAcceptMinimumValidValue() {
        assertDoesNotThrow(() -> new CourseApprovalThreshold(0.001));
    }

    @Test
    void shouldAcceptMaximumValidValue() {
        assertDoesNotThrow(() -> new CourseApprovalThreshold(100.0));
    }

    @Test
    void shouldThrowWhenValueIsZero() {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseApprovalThreshold(0.0));
        assertEquals("Approval threshold must be between 1 and 100", ex.getMessage());
    }

    @Test
    void shouldThrowWhenValueIsNegative() {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseApprovalThreshold(-1.0));
        assertEquals("Approval threshold must be between 1 and 100", ex.getMessage());
    }

    @Test
    void shouldThrowWhenValueExceedsHundred() {
        var ex = assertThrows(BusinessRuleException.class, () -> new CourseApprovalThreshold(100.001));
        assertEquals("Approval threshold must be between 1 and 100", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 50.0, 75.5, 99.9, 100.0})
    void shouldAcceptValidRange(double valid) {
        assertDoesNotThrow(() -> new CourseApprovalThreshold(valid));
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertEquals(new CourseApprovalThreshold(70.0), new CourseApprovalThreshold(70.0));
    }
}
