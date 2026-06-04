package com.codelevel.module.gamification.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XpAmountTest {

    @Test
    void shouldCreateWithPositiveValue() {
        XpAmount xp = new XpAmount(100L);
        assertEquals(100L, xp.value());
    }

    @Test
    void shouldCreateWithMinimumValue() {
        XpAmount xp = new XpAmount(1L);
        assertEquals(1L, xp.value());
    }

    @Test
    void shouldThrowWhenValueIsZero() {
        assertThrows(BusinessRuleException.class, () -> new XpAmount(0L));
    }

    @Test
    void shouldThrowWhenValueIsNegative() {
        assertThrows(BusinessRuleException.class, () -> new XpAmount(-50L));
    }

    @Test
    void shouldContainMessageOnZero() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> new XpAmount(0L));
        assertTrue(ex.getMessage().contains("greater than zero"));
    }
}
