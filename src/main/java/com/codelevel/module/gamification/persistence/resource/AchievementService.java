package com.codelevel.module.gamification.persistence.resource;

import com.codelevel.module.gamification.domain.AchievementSlug;
import com.codelevel.module.gamification.persistence.entity.AchievementEntity;
import com.codelevel.module.gamification.persistence.entity.UserAchievementEntity;
import com.codelevel.module.gamification.persistence.entity.enums.SourceXPTransaction;
import com.codelevel.module.gamification.persistence.entity.enums.TriggerAchievement;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AchievementService implements CreateUpdate {

    private final XpService xpService;

    @Inject
    public AchievementService(XpService xpService) {
        this.xpService = xpService;
    }

    @Transactional
    public AchievementEntity create(String name, String slug, String description,
                                    String iconUrl, TriggerAchievement triggerType,
                                    String triggerCriteria, Long xpReward) {
        new AchievementSlug(slug);

        try {
            long criteria = Long.parseLong(triggerCriteria);
            if (criteria < 1) throw new BusinessRuleException("Trigger criteria must be >= 1");
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Trigger criteria must be a positive integer");
        }

        if (xpReward != null && xpReward < 0)
            throw new BusinessRuleException("XP reward cannot be negative");

        AchievementEntity.findBySlug(slug).ifPresent(a -> {
            throw new ResourceAlreadyExists("Achievement with slug '" + slug + "' already exists");
        });

        AchievementEntity entity = new AchievementEntity();
        entity.setName(name);
        entity.setSlug(slug);
        entity.setDescription(description);
        entity.setIconUrl(iconUrl);
        entity.setTriggerType(triggerType);
        entity.setTriggerCriteria(triggerCriteria);
        entity.setXpReward(xpReward != null ? xpReward : 0L);
        saveOrUpdate(entity);
        return entity;
    }

    public List<AchievementEntity> list() {
        return AchievementEntity.listAllOrdered();
    }

    public AchievementEntity getById(Long id) {
        return AchievementEntity.<AchievementEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Achievement not found"));
    }

    public List<UserAchievementEntity> getUserAchievements(UUID userId) {
        return UserAchievementEntity.findByUserId(userId);
    }

    @Transactional
    public UserAchievementEntity unlock(UUID userId, Long achievementId) {
        AchievementEntity achievement = getById(achievementId);
        UserAchievementEntity unlocked = unlockInternal(userId, achievement);

        long totalAchievements = UserAchievementEntity.countByUserId(userId);
        checkAndUnlockInternal(userId, TriggerAchievement.ACHIEVEMENT_UNLOCKED, totalAchievements);

        return unlocked;
    }

    @Transactional
    public List<UserAchievementEntity> checkAndUnlock(UUID userId, TriggerAchievement triggerType, long currentValue) {
        List<UserAchievementEntity> unlocked = checkAndUnlockInternal(userId, triggerType, currentValue);

        if (!unlocked.isEmpty() && triggerType != TriggerAchievement.ACHIEVEMENT_UNLOCKED) {
            long totalAchievements = UserAchievementEntity.countByUserId(userId);
            unlocked.addAll(checkAndUnlockInternal(userId, TriggerAchievement.ACHIEVEMENT_UNLOCKED, totalAchievements));
        }

        return unlocked;
    }

    private List<UserAchievementEntity> checkAndUnlockInternal(UUID userId, TriggerAchievement triggerType, long currentValue) {
        List<AchievementEntity> candidates = AchievementEntity.findByTriggerType(triggerType);
        List<UserAchievementEntity> unlocked = new ArrayList<>();

        for (AchievementEntity achievement : candidates) {
            if (UserAchievementEntity.findByUserAndAchievement(userId, achievement.getId()).isPresent()) {
                continue;
            }
            try {
                long criteria = Long.parseLong(achievement.getTriggerCriteria());
                if (currentValue >= criteria) {
                    unlocked.add(unlockInternal(userId, achievement));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return unlocked;
    }

    private UserAchievementEntity unlockInternal(UUID userId, AchievementEntity achievement) {
        UserAchievementEntity.findByUserAndAchievement(userId, achievement.getId()).ifPresent(ua -> {
            throw new ResourceAlreadyExists("Achievement '" + achievement.getName() + "' has already been unlocked");
        });

        UserAchievementEntity entity = new UserAchievementEntity();
        entity.setUserId(userId);
        entity.setAchievementId(achievement.getId());
        saveOrUpdate(entity);

        if (achievement.getXpReward() != null && achievement.getXpReward() > 0) {
            xpService.award(userId, achievement.getXpReward(),
                    SourceXPTransaction.ACHIEVEMENT_UNLOCKED,
                    achievement.getId(),
                    "Achievement unlocked: " + achievement.getName());
        }

        return entity;
    }
}
