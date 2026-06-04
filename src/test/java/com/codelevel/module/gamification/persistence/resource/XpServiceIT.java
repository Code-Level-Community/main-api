package com.codelevel.module.gamification.persistence.resource;

import com.codelevel.module.gamification.persistence.entity.LevelEntity;
import com.codelevel.module.gamification.persistence.entity.XPTransactionEntity;
import com.codelevel.module.gamification.persistence.entity.enums.SourceXPTransaction;
import com.codelevel.module.gamification.persistence.resource.dto.AwardXpResult;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class XpServiceIT {

    @Inject
    XpService xpService;

    @Inject
    LevelService levelService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    @Test
    @TestTransaction
    void shouldAwardXp() {
        AwardXpResult result = xpService.award(uid(2001), 50L, SourceXPTransaction.LESSON_COMPLETED, 1L, "Aula concluída");

        assertNotNull(result.transaction().getId());
        assertEquals(50L, result.transaction().getXpAmount());
        assertEquals(SourceXPTransaction.LESSON_COMPLETED, result.transaction().getSource());
        assertFalse(result.leveledUp());
    }

    @Test
    @TestTransaction
    void shouldThrowOnZeroXp() {
        assertThrows(BusinessRuleException.class,
                () -> xpService.award(uid(2002), 0L, SourceXPTransaction.LESSON_COMPLETED, 2L, ""));
    }

    @Test
    @TestTransaction
    void shouldThrowOnDuplicateAward() {
        xpService.award(uid(2003), 30L, SourceXPTransaction.LESSON_COMPLETED, 10L, "Primeira vez");

        assertThrows(ResourceAlreadyExists.class,
                () -> xpService.award(uid(2003), 30L, SourceXPTransaction.LESSON_COMPLETED, 10L, "Segunda vez"));
    }

    @Test
    @TestTransaction
    void shouldReturnZeroTotalXpWithNoTransactions() {
        assertEquals(0L, xpService.getTotalXp(uid(9990)));
    }

    @Test
    @TestTransaction
    void shouldAccumulateTotalXp() {
        xpService.award(uid(2004), 100L, SourceXPTransaction.LESSON_COMPLETED, 20L, "Aula 1");
        xpService.award(uid(2004), 50L, SourceXPTransaction.EXERCISE_COMPLETED, 21L, "Exercício 1");

        assertEquals(150L, xpService.getTotalXp(uid(2004)));
    }

    @Test
    @TestTransaction
    void shouldListTransactionsByUser() {
        xpService.award(uid(2005), 40L, SourceXPTransaction.LESSON_COMPLETED, 30L, "Aula");
        xpService.award(uid(2005), 60L, SourceXPTransaction.EXERCISE_COMPLETED, 31L, "Exercício");

        List<XPTransactionEntity> transactions = xpService.listTransactions(uid(2005));

        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().allMatch(t -> t.getUserId().equals(uid(2005))));
    }

    @Test
    @TestTransaction
    void shouldDetectLevelUpOnAward() {
        levelService.create("Iniciante", 9041L, null);
        levelService.create("Aprendiz", 9100L, null);

        AwardXpResult first = xpService.award(uid(2006), 50L, SourceXPTransaction.LESSON_COMPLETED, 40L, "Aula");
        assertFalse(first.leveledUp());

        AwardXpResult second = xpService.award(uid(2006), 9060L, SourceXPTransaction.EXERCISE_COMPLETED, 41L, "Exercício");
        assertTrue(second.leveledUp());
        assertEquals("Aprendiz", second.currentLevel().map(LevelEntity::getName).orElse(null));
    }

    @Test
    @TestTransaction
    void shouldAwardXpAndReturnValidTransaction() {
        AwardXpResult result = xpService.award(uid(2007), 100L, SourceXPTransaction.LESSON_COMPLETED, 50L, "Aula");

        assertNotNull(result.transaction());
        assertEquals(100L, result.transaction().getXpAmount());
    }

    @Test
    @TestTransaction
    void shouldEnforceDailyXpCap() {
        for (int i = 1; i <= 5; i++) {
            xpService.award(uid(2008), 100L, SourceXPTransaction.LESSON_COMPLETED, (long) i, "Aula " + i);
        }

        assertThrows(BusinessRuleException.class,
                () -> xpService.award(uid(2008), 100L, SourceXPTransaction.EXERCISE_COMPLETED, 99L, "Extra"));
    }

    @Test
    @TestTransaction
    void shouldTruncateXpToRemainingCap() {
        for (int i = 1; i <= 4; i++) {
            xpService.award(uid(2009), 100L, SourceXPTransaction.LESSON_COMPLETED, (long) i, "Aula " + i);
        }

        AwardXpResult result = xpService.award(uid(2009), 200L, SourceXPTransaction.EXERCISE_COMPLETED, 99L, "Extra");

        assertEquals(100L, result.transaction().getXpAmount());
        assertEquals(500L, xpService.getTotalXp(uid(2009)));
    }

    @Test
    @TestTransaction
    void shouldSpendXp() {
        xpService.award(uid(2010), 200L, SourceXPTransaction.LESSON_COMPLETED, 1L, "Aula");

        xpService.spend(uid(2010), 100L, SourceXPTransaction.STREAK_FREEZE_COST, 1L, "Freeze");

        assertEquals(100L, xpService.getTotalXp(uid(2010)));
    }

    @Test
    @TestTransaction
    void shouldThrowSpendWhenInsufficientXp() {
        xpService.award(uid(2011), 50L, SourceXPTransaction.LESSON_COMPLETED, 1L, "Aula");

        assertThrows(BusinessRuleException.class,
                () -> xpService.spend(uid(2011), 100L, SourceXPTransaction.STREAK_FREEZE_COST, 1L, "Freeze"));
    }

    @Test
    @TestTransaction
    void shouldReturnCurrentLevel() {
        levelService.create("Iniciante", 0L, null);
        xpService.award(uid(2012), 10L, SourceXPTransaction.LESSON_COMPLETED, 1L, "Aula");

        Optional<LevelEntity> level = xpService.getCurrentLevel(uid(2012));

        assertTrue(level.isPresent());
        assertEquals("Iniciante", level.get().getName());
    }
}
