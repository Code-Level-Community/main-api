package com.codelevel.module.gamification.persistence.resource;

import com.codelevel.module.gamification.persistence.entity.AchievementEntity;
import com.codelevel.module.gamification.persistence.entity.UserAchievementEntity;
import com.codelevel.module.gamification.persistence.entity.enums.SourceXPTransaction;
import com.codelevel.module.gamification.persistence.entity.enums.TriggerAchievement;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AchievementServiceIT {

    @Inject
    AchievementService achievementService;

    @Inject
    XpService xpService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    @Test
    @TestTransaction
    void shouldCreateAchievement() {
        AchievementEntity entity = achievementService.create(
                "Primeira Aula", "primeira-aula", "Completou a primeira aula",
                null, TriggerAchievement.LESSONS_COMPLETED, "1", 50L);

        assertNotNull(entity.getId());
        assertEquals("primeira-aula", entity.getSlug());
        assertEquals(50L, entity.getXpReward());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowOnInvalidSlug() {
        assertThrows(BusinessRuleException.class, () ->
                achievementService.create("Bad", "Bad Slug!", null, null,
                        TriggerAchievement.LESSONS_COMPLETED, "1", 0L));
    }

    @Test
    @TestTransaction
    void shouldThrowOnDuplicateSlug() {
        achievementService.create("Original", "slug-unico", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "1", 0L);

        assertThrows(ResourceAlreadyExists.class, () ->
                achievementService.create("Duplicado", "slug-unico", null, null,
                        TriggerAchievement.LESSONS_COMPLETED, "5", 0L));
    }

    @Test
    @TestTransaction
    void shouldThrowOnNonNumericCriteria() {
        assertThrows(BusinessRuleException.class, () ->
                achievementService.create("Bad Criteria", "bad-criteria", null, null,
                        TriggerAchievement.LESSONS_COMPLETED, "abc", 0L));
    }

    @Test
    @TestTransaction
    void shouldThrowOnNegativeXpReward() {
        assertThrows(BusinessRuleException.class, () ->
                achievementService.create("Negative", "neg-reward", null, null,
                        TriggerAchievement.LESSONS_COMPLETED, "1", -10L));
    }

    @Test
    @TestTransaction
    void shouldListAllAchievements() {
        achievementService.create("A1", "achv-list-1", null, null, TriggerAchievement.LESSONS_COMPLETED, "1", 0L);
        achievementService.create("A2", "achv-list-2", null, null, TriggerAchievement.STREAK_DAYS, "7", 0L);

        List<AchievementEntity> list = achievementService.list();
        assertTrue(list.size() >= 2);
    }

    @Test
    @TestTransaction
    void shouldUnlockAchievement() {
        AchievementEntity achievement = achievementService.create(
                "Desbloqueador", "unlock-test", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "1", 0L);

        UserAchievementEntity unlocked = achievementService.unlock(uid(3001), achievement.getId());

        assertNotNull(unlocked.getId());
        assertEquals(uid(3001), unlocked.getUserId());
        assertEquals(achievement.getId(), unlocked.getAchievementId());
        assertNotNull(unlocked.getUnlockedAt());
    }

    @Test
    @TestTransaction
    void shouldAwardXpOnUnlock() {
        AchievementEntity achievement = achievementService.create(
                "XP Achievement", "xp-on-unlock", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "1", 100L);

        achievementService.unlock(uid(3002), achievement.getId());

        assertEquals(100L, xpService.getTotalXp(uid(3002)));
    }

    @Test
    @TestTransaction
    void shouldNotAwardXpOnUnlockWhenRewardIsZero() {
        AchievementEntity achievement = achievementService.create(
                "Honorário", "sem-xp", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "1", 0L);

        achievementService.unlock(uid(3003), achievement.getId());

        assertEquals(0L, xpService.getTotalXp(uid(3003)));
    }

    @Test
    @TestTransaction
    void shouldThrowOnDuplicateUnlock() {
        AchievementEntity achievement = achievementService.create(
                "Único", "unlock-once", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "1", 0L);

        achievementService.unlock(uid(3004), achievement.getId());

        assertThrows(ResourceAlreadyExists.class, () -> achievementService.unlock(uid(3004), achievement.getId()));
    }

    @Test
    @TestTransaction
    void shouldCheckAndUnlockWhenCriteriaReached() {
        AchievementEntity achievement = achievementService.create("10 Aulas", "ten-lessons", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "10", 0L);

        List<UserAchievementEntity> unlocked = achievementService.checkAndUnlock(
                uid(3005), TriggerAchievement.LESSONS_COMPLETED, 10L);

        assertTrue(unlocked.stream().anyMatch(ua -> ua.getAchievementId().equals(achievement.getId())));
        assertTrue(unlocked.stream().allMatch(ua -> ua.getUserId().equals(uid(3005))));
    }

    @Test
    @TestTransaction
    void shouldNotUnlockWhenCriteriaNotMet() {
        AchievementEntity achievement = achievementService.create("50 Aulas", "fifty-lessons", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "50", 0L);

        List<UserAchievementEntity> unlocked = achievementService.checkAndUnlock(
                uid(3006), TriggerAchievement.LESSONS_COMPLETED, 5L);

        assertFalse(unlocked.stream().anyMatch(ua -> ua.getAchievementId().equals(achievement.getId())));
    }

    @Test
    @TestTransaction
    void shouldNotUnlockAlreadyUnlockedAchievement() {
        AchievementEntity achievement = achievementService.create(
                "Já desbloqueado", "already-done", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "1", 0L);

        achievementService.unlock(uid(3007), achievement.getId());

        List<UserAchievementEntity> unlocked = achievementService.checkAndUnlock(
                uid(3007), TriggerAchievement.LESSONS_COMPLETED, 5L);

        assertFalse(unlocked.stream().anyMatch(ua -> ua.getAchievementId().equals(achievement.getId())));
    }

    @Test
    @TestTransaction
    void shouldUnlockCascadeAchievementAfterNthUnlock() {
        achievementService.create("Colecionador", "colecionador", null, null,
                TriggerAchievement.ACHIEVEMENT_UNLOCKED, "2", 0L);
        achievementService.create("Primeira Aula", "cascade-lesson-1", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "1", 0L);
        achievementService.create("Segunda Aula", "cascade-lesson-2", null, null,
                TriggerAchievement.LESSONS_COMPLETED, "2", 0L);

        achievementService.checkAndUnlock(uid(3008), TriggerAchievement.LESSONS_COMPLETED, 1L);
        List<UserAchievementEntity> second = achievementService.checkAndUnlock(
                uid(3008), TriggerAchievement.LESSONS_COMPLETED, 2L);

        long colecionadorCount = achievementService.getUserAchievements(uid(3008))
                .stream().filter(ua -> {
                    AchievementEntity a = achievementService.getById(ua.getAchievementId());
                    return "colecionador".equals(a.getSlug());
                }).count();
        assertEquals(1L, colecionadorCount);
    }
}
