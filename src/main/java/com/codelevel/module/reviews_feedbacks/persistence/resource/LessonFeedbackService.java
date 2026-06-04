package com.codelevel.module.reviews_feedbacks.persistence.resource;

import com.codelevel.module.reviews_feedbacks.domain.ReviewComment;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.LessonFeedbackCreateRequest;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.LessonFeedbackUpdateRequest;
import com.codelevel.module.reviews_feedbacks.persistence.entity.LessonFeedbackEntity;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LessonFeedbackService {

    private static final Logger log = Logger.getLogger(LessonFeedbackService.class);

    @Transactional
    public LessonFeedbackEntity create(LessonFeedbackCreateRequest request, UUID userId) {
        new ReviewComment(request.comment());

        LessonFeedbackEntity.findByUserAndLesson(userId, request.lessonId()).ifPresent(existing -> {
            throw new ResourceAlreadyExists("You have already submitted feedback for this lesson");
        });

        LessonFeedbackEntity entity = new LessonFeedbackEntity();
        entity.setUserId(userId);
        entity.setLessonId(request.lessonId());
        entity.setHelpful(request.isHelpful());
        entity.setComment(request.comment());
        entity.persist();

        log.infof("LessonFeedback created: userId=%s, lessonId=%d", userId, request.lessonId());
        return entity;
    }

    @Transactional
    public LessonFeedbackEntity update(Long id, LessonFeedbackUpdateRequest request, UUID userId) {
        new ReviewComment(request.comment());

        LessonFeedbackEntity entity = findById(id);

        if (!entity.getUserId().equals(userId)) {
            throw new BusinessRuleException("You do not have permission to edit this feedback");
        }

        entity.setHelpful(request.isHelpful());
        entity.setComment(request.comment());
        entity.persist();

        return entity;
    }

    @Transactional
    public void delete(Long id, UUID userId, boolean isAdmin) {
        LessonFeedbackEntity entity = findById(id);

        if (!isAdmin && !entity.getUserId().equals(userId)) {
            throw new BusinessRuleException("You do not have permission to delete this feedback");
        }

        entity.delete();
        log.infof("LessonFeedback deleted: id=%d by userId=%s (isAdmin=%b)", id, userId, isAdmin);
    }

    public LessonFeedbackEntity findById(Long id) {
        return LessonFeedbackEntity.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Feedback not found"));
    }

    public List<LessonFeedbackEntity> findByLesson(Long lessonId) {
        return LessonFeedbackEntity.findByLessonId(lessonId);
    }

    public List<LessonFeedbackEntity> findByUser(UUID userId) {
        return LessonFeedbackEntity.findByUserId(userId);
    }
}
