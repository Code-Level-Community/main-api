package com.codelevel.module.gamification.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StreakDaysTest {

    @Test
    void shouldCreateWithZero() {
        StreakDays streak = new StreakDays(0L);
        assertEquals(0L, streak.value());
    }

    @Test
    void shouldCreateWithPositiveValue() {
        StreakDays streak = new StreakDays(30L);
        assertEquals(30L, streak.value());
    }

    @Test
    void shouldThrowWhenValueIsNegative() {
        assertThrows(BusinessRuleException.class, () -> new StreakDays(-1L));
    }

    @Test
    void shouldContainMessageOnNegative() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> new StreakDays(-5L));
        assertTrue(ex.getMessage().contains("negative"));
    }
}
