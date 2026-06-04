package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.ExerciseCreateRequest;
import com.codelevel.module.course.http.rest.dto.ExerciseUpdateRequest;
import com.codelevel.module.course.persistence.entity.ExerciseEntity;
import com.codelevel.module.course.persistence.entity.enums.ExerciseType;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ExerciseService implements CreateUpdate {

    private static final long DEFAULT_XP_REWARD = 100L;

    @Transactional
    public ExerciseEntity create(Long lessonId, ExerciseCreateRequest request) {
        ExerciseEntity entity = new ExerciseEntity();
        entity.setLessonId(lessonId);
        entity.setType(ExerciseType.valueOf(request.type()));
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setQuizData(request.quizData());
        entity.setCodeTemplate(request.codeTemplate());
        entity.setTestCases(request.testCases());
        entity.setMaxAttempts(request.maxAttempts());
        entity.setXpReward(request.xpReward() != null ? request.xpReward() : DEFAULT_XP_REWARD);
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public ExerciseEntity update(Long id, ExerciseUpdateRequest request) {
        ExerciseEntity entity = getById(id);
        if (request.type() != null) {
            entity.setType(ExerciseType.valueOf(request.type()));
        }
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setQuizData(request.quizData());
        entity.setCodeTemplate(request.codeTemplate());
        entity.setTestCases(request.testCases());
        entity.setMaxAttempts(request.maxAttempts());
        entity.setXpReward(request.xpReward() != null ? request.xpReward() : entity.getXpReward());
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public void delete(Long id) {
        ExerciseEntity entity = getById(id);
        entity.delete();
    }

    public ExerciseEntity getById(Long id) {
        return ExerciseEntity.<ExerciseEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Exercise not found"));
    }

    public List<ExerciseEntity> listByLesson(Long lessonId) {
        return ExerciseEntity.findByLessonId(lessonId);
    }
}
