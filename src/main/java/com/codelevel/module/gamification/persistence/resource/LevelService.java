package com.codelevel.module.gamification.persistence.resource;

import com.codelevel.module.gamification.persistence.entity.LevelEntity;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LevelService implements CreateUpdate {

    @Transactional
    public LevelEntity create(String name, Long xpRequired, String badgeIconUrl) {
        if (xpRequired == null || xpRequired < 0)
            throw new BusinessRuleException("XP required must be greater than or equal to zero");

        LevelEntity.findLevelForXp(xpRequired)
                .filter(l -> l.getXpRequired().equals(xpRequired))
                .ifPresent(l -> {
                    throw new ResourceAlreadyExists("A level with " + xpRequired + " XP required already exists");
                });

        LevelEntity entity = new LevelEntity();
        entity.setName(name);
        entity.setXpRequired(xpRequired);
        entity.setBadgeIconUrl(badgeIconUrl);
        saveOrUpdate(entity);
        return entity;
    }

    public List<LevelEntity> list() {
        return LevelEntity.listAllOrdered();
    }

    public Optional<LevelEntity> getLevelForXp(long totalXp) {
        return LevelEntity.findLevelForXp(totalXp);
    }
}
