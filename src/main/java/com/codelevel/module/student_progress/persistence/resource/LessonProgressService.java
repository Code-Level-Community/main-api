package com.codelevel.module.student_progress.persistence.resource;

import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.module.student_progress.persistence.entity.LessonProgressEntity;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LessonProgressService implements CreateUpdate {

    @Transactional
    public LessonProgressEntity trackProgress(UUID userId, Long lessonId, Long watchTimeSeconds, Long videoDurationSeconds) {
        LessonProgressEntity entity = LessonProgressEntity.findByUserAndLesson(userId, lessonId)
                .orElseGet(() -> {
                    LessonProgressEntity newEntity = new LessonProgressEntity();
                    newEntity.setUserId(userId);
                    newEntity.setLessonId(lessonId);
                    return newEntity;
                });

        entity.setWatchTimeSeconds(watchTimeSeconds);
        entity.setVideoDurationSeconds(videoDurationSeconds);
        entity.setLastWatchedAt(LocalDateTime.now());

        if (videoDurationSeconds != null && videoDurationSeconds > 0) {
            double percentage = (double) watchTimeSeconds / videoDurationSeconds * 100.0;
            entity.setCompletionPercentage(Math.min(100.0, percentage));

            if (!entity.isCompleted() && watchTimeSeconds >= videoDurationSeconds * 0.9) {
                entity.setCompleted(true);
                entity.setCompletedAt(LocalDateTime.now());
            }
        }

        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public LessonProgressEntity markComplete(UUID userId, Long lessonId) {
        LessonProgressEntity entity = LessonProgressEntity.findByUserAndLesson(userId, lessonId)
                .orElseGet(() -> {
                    LessonProgressEntity newEntity = new LessonProgressEntity();
                    newEntity.setUserId(userId);
                    newEntity.setLessonId(lessonId);
                    return newEntity;
                });

        entity.setCompleted(true);
        entity.setCompletedAt(LocalDateTime.now());
        entity.setCompletionPercentage(100.0);
        saveOrUpdate(entity);
        return entity;
    }

    public LessonProgressEntity getByUserAndLesson(UUID userId, Long lessonId) {
        return LessonProgressEntity.findByUserAndLesson(userId, lessonId)
                .orElseThrow(() -> new ResourceNotFound("Lesson progress not found"));
    }

    public List<LessonProgressEntity> listByUserId(UUID userId) {
        return LessonProgressEntity.findByUserId(userId);
    }
}