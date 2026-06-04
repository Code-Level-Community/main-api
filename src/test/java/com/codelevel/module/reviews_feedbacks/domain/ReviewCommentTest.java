package com.codelevel.module.reviews_feedbacks.domain;

import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCommentTest {

    @Test
    void shouldAcceptNullComment() {
        ReviewComment comment = new ReviewComment(null);
        assertNull(comment.text());
    }

    @Test
    void shouldAcceptEmptyComment() {
        ReviewComment comment = new ReviewComment("");
        assertEquals("", comment.text());
    }

    @Test
    void shouldAcceptCommentWithMaxLength() {
        String maxComment = "a".repeat(1000);
        ReviewComment comment = new ReviewComment(maxComment);
        assertEquals(1000, comment.text().length());
    }

    @Test
    void shouldAcceptValidComment() {
        ReviewComment comment = new ReviewComment("Excelente conteúdo!");
        assertEquals("Excelente conteúdo!", comment.text());
    }

    @Test
    void shouldRejectCommentExceedingMaxLength() {
        String tooLong = "a".repeat(1001);
        assertThrows(BusinessRuleException.class, () -> new ReviewComment(tooLong));
    }

    @Test
    void shouldThrowWithCorrectMessage() {
        String tooLong = "x".repeat(1001);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> new ReviewComment(tooLong));
        assertEquals("Comment cannot exceed 1000 characters", ex.getMessage());
    }
}
