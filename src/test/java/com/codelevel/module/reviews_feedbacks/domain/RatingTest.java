package com.codelevel.module.reviews_feedbacks.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RatingTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void shouldAcceptValidRatings(int score) {
        Rating rating = new Rating(score);
        assertEquals(score, rating.score());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1, 100})
    void shouldRejectInvalidRatings(int score) {
        assertThrows(BusinessRuleException.class, () -> new Rating(score));
    }

    @Test
    void shouldThrowWithCorrectMessageOnRatingTooHigh() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> new Rating(6));
        assertEquals("Rating must be between 1 and 5", ex.getMessage());
    }

    @Test
    void shouldThrowWithCorrectMessageOnRatingTooLow() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> new Rating(0));
        assertEquals("Rating must be between 1 and 5", ex.getMessage());
    }
}
