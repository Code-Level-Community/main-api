package com.codelevel.module.student_progress.persistence.resource;

import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.module.student_progress.persistence.entity.ExerciseAttemptEntity;
import com.codelevel.shared.exception.BusinessRuleException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ExerciseAttemptService implements CreateUpdate {

    @Transactional
    public ExerciseAttemptEntity submit(UUID userId, Long exerciseId, String submittedAnswer,
                                        boolean correct, Long baseXpReward, int maxAttempts, String feedback) {
        long totalAttemptsBefore = ExerciseAttemptEntity.countByUserAndExercise(userId, exerciseId);

        if (maxAttempts > 0 && totalAttemptsBefore >= maxAttempts) {
            throw new BusinessRuleException("Maximum number of attempts reached for this exercise");
        }

        boolean alreadyCorrect = ExerciseAttemptEntity.hasCorrectAttempt(userId, exerciseId);

        long xpEarned = 0L;
        if (correct && !alreadyCorrect) {
            long failedAttemptsBefore = ExerciseAttemptEntity.countFailedByUserAndExercise(userId, exerciseId);
            double multiplier = Math.max(0.0, 1.0 - failedAttemptsBefore * 0.10);
            xpEarned = Math.round(baseXpReward * multiplier);
        }

        ExerciseAttemptEntity entity = new ExerciseAttemptEntity();
        entity.setUserId(userId);
        entity.setExerciseId(exerciseId);
        entity.setAttemptNumber(totalAttemptsBefore + 1);
        entity.setSubmittedAnswer(submittedAnswer);
        entity.setCorrect(correct);
        entity.setXpEarned(xpEarned);
        entity.setFeedback(feedback);
        saveOrUpdate(entity);
        return entity;
    }

    public List<ExerciseAttemptEntity> listByUserAndExercise(UUID userId, Long exerciseId) {
        return ExerciseAttemptEntity.findByUserAndExercise(userId, exerciseId);
    }

    public List<ExerciseAttemptEntity> listByUserId(UUID userId) {
        return ExerciseAttemptEntity.findByUserId(userId);
    }
}