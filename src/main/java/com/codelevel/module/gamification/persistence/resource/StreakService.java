package com.codelevel.module.gamification.persistence.resource;

import com.codelevel.module.gamification.domain.StreakDays;
import com.codelevel.module.gamification.persistence.entity.UserStreakEntity;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class StreakService implements CreateUpdate {

    @Transactional
    public UserStreakEntity recordActivity(UUID userId) {
        Optional<UserStreakEntity> existing = UserStreakEntity.findByUserId(userId);

        if (existing.isEmpty()) {
            return createStreak(userId);
        }

        UserStreakEntity entity = existing.get();
        LocalDate today = LocalDate.now();
        LocalDate lastActivity = entity.getLastActivityDate();

        if (lastActivity.equals(today)) {
            return entity;
        }

        long dayGap = ChronoUnit.DAYS.between(lastActivity, today);

        if (dayGap == 1) {
            entity.setCurrentStreakDays(entity.getCurrentStreakDays() + 1);
            entity.setFreezeUsedAt(null);
        } else if (dayGap == 2 && entity.getFreezeUsedAt() != null) {
            entity.setCurrentStreakDays(entity.getCurrentStreakDays() + 1);
            entity.setFreezeUsedAt(null);
        } else {
            entity.setCurrentStreakDays(1L);
            entity.setFreezeUsedAt(null);
        }

        if (entity.getCurrentStreakDays() > entity.getLongestStreakDays()) {
            entity.setLongestStreakDays(entity.getCurrentStreakDays());
        }

        new StreakDays(entity.getCurrentStreakDays());

        entity.setLastActivityDate(today);
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public UserStreakEntity activateFreeze(UUID userId) {
        UserStreakEntity entity = UserStreakEntity.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFound("Streak not found. Record an activity first."));

        if (entity.getFreezeUsedAt() != null && !entity.getFreezeUsedAt().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Streak freeze is already active");
        }

        entity.setFreezeUsedAt(LocalDate.now());
        saveOrUpdate(entity);
        return entity;
    }

    public Optional<UserStreakEntity> getStreak(UUID userId) {
        return UserStreakEntity.findByUserId(userId);
    }

    private UserStreakEntity createStreak(UUID userId) {
        UserStreakEntity entity = new UserStreakEntity();
        entity.setUserId(userId);
        entity.setCurrentStreakDays(1L);
        entity.setLastActivityDate(LocalDate.now());
        entity.setLongestStreakDays(1L);
        saveOrUpdate(entity);
        return entity;
    }
}
