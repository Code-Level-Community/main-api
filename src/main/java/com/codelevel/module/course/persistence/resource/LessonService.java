package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.LessonCreateRequest;
import com.codelevel.module.course.http.rest.dto.LessonUpdateRequest;
import com.codelevel.module.course.persistence.entity.LessonEntity;
import com.codelevel.module.course.persistence.entity.enums.TypeContentLesson;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class LessonService implements CreateUpdate {

    private static final long DEFAULT_XP_REWARD = 50L;

    @Transactional
    public LessonEntity create(Long moduleId, LessonCreateRequest request) {
        LessonEntity entity = new LessonEntity();
        entity.setModuleId(moduleId);
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        if (request.contentType() != null) {
            entity.setContentType(TypeContentLesson.valueOf(request.contentType()));
        }
        entity.setVideoUrl(request.videoUrl());
        entity.setVideoDurationSeconds(request.videoDurationSeconds());
        entity.setTextContent(request.textContent());
        entity.setOrderPosition(request.orderPosition());
        entity.setXpReward(request.xpReward() != null ? request.xpReward() : DEFAULT_XP_REWARD);
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public LessonEntity update(Long id, LessonUpdateRequest request) {
        LessonEntity entity = getById(id);
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        if (request.contentType() != null) {
            entity.setContentType(TypeContentLesson.valueOf(request.contentType()));
        }
        entity.setVideoUrl(request.videoUrl());
        entity.setVideoDurationSeconds(request.videoDurationSeconds());
        entity.setTextContent(request.textContent());
        entity.setOrderPosition(request.orderPosition());
        entity.setXpReward(request.xpReward() != null ? request.xpReward() : entity.getXpReward());
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public void delete(Long id) {
        LessonEntity entity = getById(id);
        entity.delete();
    }

    public LessonEntity getById(Long id) {
        return LessonEntity.<LessonEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Lesson not found"));
    }

    public List<LessonEntity> listByModule(Long moduleId) {
        return LessonEntity.findByModuleId(moduleId);
    }
}
