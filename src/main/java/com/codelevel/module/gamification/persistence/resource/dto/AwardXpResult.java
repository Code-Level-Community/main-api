package com.codelevel.module.gamification.persistence.resource.dto;

import com.codelevel.module.gamification.persistence.entity.LevelEntity;
import com.codelevel.module.gamification.persistence.entity.XPTransactionEntity;

import java.util.Optional;

public record AwardXpResult(
        XPTransactionEntity transaction,
        Optional<LevelEntity> currentLevel,
        boolean leveledUp
) {}
