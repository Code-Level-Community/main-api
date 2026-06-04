package com.codelevel.module.gamification.persistence.resource;

import com.codelevel.module.gamification.domain.XpAmount;
import com.codelevel.module.gamification.persistence.entity.LevelEntity;
import com.codelevel.module.gamification.persistence.entity.XPTransactionEntity;
import com.codelevel.module.gamification.persistence.entity.enums.SourceXPTransaction;
import com.codelevel.module.gamification.persistence.resource.dto.AwardXpResult;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class XpService implements CreateUpdate {

    static final long DAILY_XP_CAP = 500L;
    static final long STREAK_BONUS_THRESHOLD = 7L;
    static final double STREAK_BONUS_MULTIPLIER = 1.5;

    private final StreakService streakService;

    @Inject
    public XpService(StreakService streakService) {
        this.streakService = streakService;
    }

    @Transactional
    public AwardXpResult award(UUID userId, Long amount, SourceXPTransaction source, Long sourceId, String description) {
        new XpAmount(amount);

        XPTransactionEntity.findByUserSourceAndSourceId(userId, source, sourceId).ifPresent(t -> {
            throw new ResourceAlreadyExists("XP already awarded for this action");
        });

        long streakBonus = calculateStreakBonus(userId, amount);
        long finalAmount = streakBonus > 0 ? streakBonus : amount;

        long dailyUsed = XPTransactionEntity.sumXpByUserIdToday(userId);
        long remaining = DAILY_XP_CAP - dailyUsed;

        if (remaining <= 0) {
            throw new BusinessRuleException("Daily XP cap reached (max " + DAILY_XP_CAP + " XP/day)");
        }

        long effectiveAmount = Math.min(finalAmount, remaining);

        Optional<LevelEntity> levelBefore = LevelEntity.findLevelForXp(XPTransactionEntity.sumXpByUserId(userId));

        XPTransactionEntity entity = new XPTransactionEntity();
        entity.setUserId(userId);
        entity.setXpAmount(effectiveAmount);
        entity.setSource(source);
        entity.setSourceId(sourceId);
        entity.setDescription(description);
        saveOrUpdate(entity);

        long totalXpAfter = XPTransactionEntity.sumXpByUserId(userId);
        Optional<LevelEntity> levelAfter = LevelEntity.findLevelForXp(totalXpAfter);

        boolean leveledUp = levelBefore.map(LevelEntity::getId).map(id -> !id.equals(levelAfter.map(LevelEntity::getId).orElse(null))).orElse(levelAfter.isPresent());

        return new AwardXpResult(entity, levelAfter, leveledUp);
    }

    @Transactional
    public XPTransactionEntity spend(UUID userId, long amount, SourceXPTransaction source, Long sourceId, String description) {
        long total = XPTransactionEntity.sumXpByUserId(userId);
        if (total < amount) {
            throw new BusinessRuleException("Insufficient XP. Current balance: " + total + " XP");
        }

        XPTransactionEntity.findByUserSourceAndSourceId(userId, source, sourceId).ifPresent(t -> {
            throw new ResourceAlreadyExists("Streak freeze already used today");
        });

        XPTransactionEntity entity = new XPTransactionEntity();
        entity.setUserId(userId);
        entity.setXpAmount(-amount);
        entity.setSource(source);
        entity.setSourceId(sourceId);
        entity.setDescription(description);
        saveOrUpdate(entity);
        return entity;
    }

    public long getTotalXp(UUID userId) {
        return XPTransactionEntity.sumXpByUserId(userId);
    }

    public List<XPTransactionEntity> listTransactions(UUID userId) {
        return XPTransactionEntity.findByUserId(userId);
    }

    public Optional<LevelEntity> getCurrentLevel(UUID userId) {
        long totalXp = getTotalXp(userId);
        return LevelEntity.findLevelForXp(totalXp);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getLeaderboardRaw(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return Panache.getEntityManager()
                .createQuery(
                        "SELECT x.userId, SUM(x.xpAmount) as total FROM XPTransactionEntity x " +
                        "GROUP BY x.userId ORDER BY total DESC"
                )
                .setMaxResults(safeLimit)
                .getResultList();
    }

    private long calculateStreakBonus(UUID userId, long baseAmount) {
        return streakService.getStreak(userId)
                .filter(s -> s.getCurrentStreakDays() >= STREAK_BONUS_THRESHOLD)
                .map(s -> Math.round(baseAmount * STREAK_BONUS_MULTIPLIER))
                .orElse(0L);
    }
}
