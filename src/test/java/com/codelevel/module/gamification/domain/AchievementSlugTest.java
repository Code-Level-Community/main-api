package com.codelevel.module.gamification.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AchievementSlugTest {

    @Test
    void shouldCreateValidSlug() {
        AchievementSlug slug = new AchievementSlug("first-lesson");
        assertEquals("first-lesson", slug.value());
    }

    @Test
    void shouldAcceptNumbersAndHyphens() {
        AchievementSlug slug = new AchievementSlug("streak-7-days");
        assertEquals("streak-7-days", slug.value());
    }

    @Test
    void shouldThrowOnUppercase() {
        assertThrows(BusinessRuleException.class, () -> new AchievementSlug("First-Lesson"));
    }

    @Test
    void shouldThrowOnSpace() {
        assertThrows(BusinessRuleException.class, () -> new AchievementSlug("first lesson"));
    }

    @Test
    void shouldThrowOnTooShort() {
        assertThrows(BusinessRuleException.class, () -> new AchievementSlug("a"));
    }

    @Test
    void shouldThrowOnEmpty() {
        assertThrows(BusinessRuleException.class, () -> new AchievementSlug(""));
    }

    @Test
    void shouldThrowOnNull() {
        assertThrows(BusinessRuleException.class, () -> new AchievementSlug(null));
    }

    @Test
    void shouldThrowOnSpecialCharacters() {
        assertThrows(BusinessRuleException.class, () -> new AchievementSlug("first_lesson"));
    }
}
