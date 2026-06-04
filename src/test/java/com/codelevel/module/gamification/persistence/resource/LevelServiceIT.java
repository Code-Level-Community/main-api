package com.codelevel.module.gamification.persistence.resource;

import com.codelevel.module.gamification.persistence.entity.LevelEntity;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LevelServiceIT {

    @Inject
    LevelService levelService;

    @Test
    @TestTransaction
    void shouldCreateLevel() {
        LevelEntity entity = levelService.create("Iniciante", 0L, "https://cdn.example.com/badges/iniciante.png");

        assertNotNull(entity.getId());
        assertEquals("Iniciante", entity.getName());
        assertEquals(0L, entity.getXpRequired());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowOnNegativeXpRequired() {
        assertThrows(BusinessRuleException.class, () -> levelService.create("Level", -1L, null));
    }

    @Test
    @TestTransaction
    void shouldThrowOnDuplicateXpRequired() {
        levelService.create("Nível A", 9001L, null);
        assertThrows(ResourceAlreadyExists.class, () -> levelService.create("Nível B", 9001L, null));
    }

    @Test
    @TestTransaction
    void shouldListLevelsOrderedByXpRequired() {
        levelService.create("Avançado", 500L, null);
        levelService.create("Iniciante", 0L, null);
        levelService.create("Intermediário", 200L, null);

        List<LevelEntity> levels = levelService.list();

        assertTrue(levels.size() >= 3);
        for (int i = 1; i < levels.size(); i++) {
            assertTrue(levels.get(i).getXpRequired() >= levels.get(i - 1).getXpRequired());
        }
    }

    @Test
    @TestTransaction
    void shouldGetLevelForExactXp() {
        levelService.create("Bronze", 0L, null);
        levelService.create("Prata", 200L, null);
        levelService.create("Ouro", 500L, null);

        Optional<LevelEntity> level = levelService.getLevelForXp(200L);

        assertTrue(level.isPresent());
        assertEquals("Prata", level.get().getName());
    }

    @Test
    @TestTransaction
    void shouldGetHighestMatchingLevel() {
        levelService.create("Nível 1", 9010L, null);
        levelService.create("Nível 2", 9020L, null);
        levelService.create("Nível 3", 9030L, null);

        Optional<LevelEntity> level = levelService.getLevelForXp(9035L);

        assertTrue(level.isPresent());
        assertEquals("Nível 3", level.get().getName());
    }

    @Test
    @TestTransaction
    void shouldReturnEmptyWhenXpBelowAllLevels() {
        levelService.create("Primeiro Nível", 50L, null);

        Optional<LevelEntity> level = levelService.getLevelForXp(10L);

        assertTrue(level.isEmpty());
    }
}
