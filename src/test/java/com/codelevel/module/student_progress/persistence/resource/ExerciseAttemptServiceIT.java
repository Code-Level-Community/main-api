package com.codelevel.module.student_progress.persistence.resource;

import com.codelevel.module.student_progress.persistence.entity.ExerciseAttemptEntity;
import com.codelevel.shared.exception.BusinessRuleException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ExerciseAttemptServiceIT {

    @Inject
    ExerciseAttemptService exerciseAttemptService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    @Test
    @TestTransaction
    void shouldSubmitCorrectFirstAttemptWithFullXp() {
        ExerciseAttemptEntity entity = exerciseAttemptService.submit(uid(1), 1001L, "42", true, 100L, 0, null);

        assertTrue(entity.isCorrect());
        assertEquals(1L, entity.getAttemptNumber());
        assertEquals(100L, entity.getXpEarned());
        assertNotNull(entity.getSubmittedAt());
    }

    @Test
    @TestTransaction
    void shouldReduceXpByTenPercentPerFailedAttempt() {
        exerciseAttemptService.submit(uid(2), 2001L, "wrong", false, 100L, 0, null);
        exerciseAttemptService.submit(uid(2), 2001L, "wrong2", false, 100L, 0, null);
        ExerciseAttemptEntity correct = exerciseAttemptService.submit(uid(2), 2001L, "right", true, 100L, 0, null);

        assertEquals(80L, correct.getXpEarned());
        assertEquals(3L, correct.getAttemptNumber());
    }

    @Test
    @TestTransaction
    void shouldGiveZeroXpWhenUserAlreadyAnsweredCorrectly() {
        exerciseAttemptService.submit(uid(3), 3001L, "right", true, 100L, 0, null);
        ExerciseAttemptEntity retry = exerciseAttemptService.submit(uid(3), 3001L, "right again", true, 100L, 0, null);

        assertEquals(0L, retry.getXpEarned());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenMaxAttemptsExceeded() {
        exerciseAttemptService.submit(uid(4), 4001L, "a1", false, 50L, 2, null);
        exerciseAttemptService.submit(uid(4), 4001L, "a2", false, 50L, 2, null);

        assertThrows(BusinessRuleException.class,
                () -> exerciseAttemptService.submit(uid(4), 4001L, "a3", true, 50L, 2, null));
    }

    @Test
    @TestTransaction
    void shouldAllowUnlimitedAttemptsWhenMaxAttemptsIsZero() {
        for (int i = 0; i < 15; i++) {
            exerciseAttemptService.submit(uid(5), 5001L, "answer" + i, false, 20L, 0, null);
        }
        ExerciseAttemptEntity last = exerciseAttemptService.submit(uid(5), 5001L, "final", true, 20L, 0, null);

        assertEquals(16L, last.getAttemptNumber());
        assertEquals(0L, last.getXpEarned());
    }

    @Test
    @TestTransaction
    void shouldTrackAttemptNumbersSequentially() {
        ExerciseAttemptEntity a1 = exerciseAttemptService.submit(uid(6), 6001L, "try1", false, 30L, 0, "Feedback 1");
        ExerciseAttemptEntity a2 = exerciseAttemptService.submit(uid(6), 6001L, "try2", false, 30L, 0, "Feedback 2");
        ExerciseAttemptEntity a3 = exerciseAttemptService.submit(uid(6), 6001L, "try3", true, 30L, 0, "Correto!");

        assertEquals(1L, a1.getAttemptNumber());
        assertEquals(2L, a2.getAttemptNumber());
        assertEquals(3L, a3.getAttemptNumber());
    }

    @Test
    @TestTransaction
    void shouldListAttemptsByUserAndExercise() {
        exerciseAttemptService.submit(uid(7), 7001L, "ans1", false, 40L, 0, null);
        exerciseAttemptService.submit(uid(7), 7001L, "ans2", true, 40L, 0, null);

        List<ExerciseAttemptEntity> list = exerciseAttemptService.listByUserAndExercise(uid(7), 7001L);

        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getAttemptNumber());
        assertEquals(2L, list.get(1).getAttemptNumber());
    }

    @Test
    @TestTransaction
    void shouldGiveZeroXpWhenAllFailuresExhaustMultiplier() {
        for (int i = 0; i < 10; i++) {
            exerciseAttemptService.submit(uid(8), 8001L, "fail", false, 100L, 0, null);
        }
        ExerciseAttemptEntity correct = exerciseAttemptService.submit(uid(8), 8001L, "right", true, 100L, 0, null);

        assertEquals(0L, correct.getXpEarned());
    }
}
