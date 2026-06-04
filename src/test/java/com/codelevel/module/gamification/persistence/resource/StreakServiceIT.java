package com.codelevel.module.gamification.persistence.resource;

import com.codelevel.module.gamification.persistence.entity.UserStreakEntity;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceNotFound;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class StreakServiceIT {

    @Inject
    StreakService streakService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    @Test
    @TestTransaction
    void shouldCreateStreakOnFirstActivity() {
        UserStreakEntity streak = streakService.recordActivity(uid(1001));

        assertEquals(uid(1001), streak.getUserId());
        assertEquals(1L, streak.getCurrentStreakDays());
        assertEquals(1L, streak.getLongestStreakDays());
        assertEquals(LocalDate.now(), streak.getLastActivityDate());
        assertNull(streak.getFreezeUsedAt());
    }

    @Test
    @TestTransaction
    void shouldBeIdempotentOnSameDay() {
        streakService.recordActivity(uid(1002));
        UserStreakEntity second = streakService.recordActivity(uid(1002));

        assertEquals(1L, second.getCurrentStreakDays());
        assertEquals(LocalDate.now(), second.getLastActivityDate());
    }

    @Test
    @TestTransaction
    void shouldExtendStreakOnConsecutiveDay() {
        UserStreakEntity streak = streakService.recordActivity(uid(1003));
        streak.setLastActivityDate(LocalDate.now().minusDays(1));
        streak.persistAndFlush();

        UserStreakEntity extended = streakService.recordActivity(uid(1003));

        assertEquals(2L, extended.getCurrentStreakDays());
        assertEquals(2L, extended.getLongestStreakDays());
    }

    @Test
    @TestTransaction
    void shouldResetStreakAfterGap() {
        UserStreakEntity streak = streakService.recordActivity(uid(1004));
        streak.setCurrentStreakDays(10L);
        streak.setLongestStreakDays(10L);
        streak.setLastActivityDate(LocalDate.now().minusDays(3));
        streak.persistAndFlush();

        UserStreakEntity reset = streakService.recordActivity(uid(1004));

        assertEquals(1L, reset.getCurrentStreakDays());
        assertEquals(10L, reset.getLongestStreakDays());
    }

    @Test
    @TestTransaction
    void shouldUpdateLongestWhenCurrentExceedsIt() {
        UserStreakEntity streak = streakService.recordActivity(uid(1005));
        streak.setCurrentStreakDays(4L);
        streak.setLongestStreakDays(4L);
        streak.setLastActivityDate(LocalDate.now().minusDays(1));
        streak.persistAndFlush();

        UserStreakEntity extended = streakService.recordActivity(uid(1005));

        assertEquals(5L, extended.getCurrentStreakDays());
        assertEquals(5L, extended.getLongestStreakDays());
    }

    @Test
    @TestTransaction
    void shouldProtectStreakWithFreezeOnExactlyOneDayGap() {
        UserStreakEntity streak = streakService.recordActivity(uid(1006));
        streak.setCurrentStreakDays(5L);
        streak.setLongestStreakDays(5L);
        streak.setLastActivityDate(LocalDate.now().minusDays(2));
        streak.setFreezeUsedAt(LocalDate.now().minusDays(1));
        streak.persistAndFlush();

        UserStreakEntity result = streakService.recordActivity(uid(1006));

        assertEquals(6L, result.getCurrentStreakDays());
        assertNull(result.getFreezeUsedAt());
    }

    @Test
    @TestTransaction
    void shouldNotProtectStreakWithFreezeOnGapLargerThanOneDay() {
        UserStreakEntity streak = streakService.recordActivity(uid(1007));
        streak.setCurrentStreakDays(5L);
        streak.setLastActivityDate(LocalDate.now().minusDays(3));
        streak.setFreezeUsedAt(LocalDate.now().minusDays(1));
        streak.persistAndFlush();

        UserStreakEntity result = streakService.recordActivity(uid(1007));

        assertEquals(1L, result.getCurrentStreakDays());
        assertNull(result.getFreezeUsedAt());
    }

    @Test
    @TestTransaction
    void shouldReturnEmptyForUserWithNoStreak() {
        Optional<UserStreakEntity> streak = streakService.getStreak(uid(9999));
        assertTrue(streak.isEmpty());
    }

    @Test
    @TestTransaction
    void shouldActivateFreeze() {
        streakService.recordActivity(uid(1008));
        UserStreakEntity streak = streakService.activateFreeze(uid(1008));

        assertEquals(LocalDate.now(), streak.getFreezeUsedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowActivateFreezeWhenAlreadyActive() {
        streakService.recordActivity(uid(1009));
        streakService.activateFreeze(uid(1009));

        assertThrows(BusinessRuleException.class, () -> streakService.activateFreeze(uid(1009)));
    }

    @Test
    @TestTransaction
    void shouldThrowActivateFreezeWhenNoStreak() {
        assertThrows(ResourceNotFound.class, () -> streakService.activateFreeze(uid(9998)));
    }
}
