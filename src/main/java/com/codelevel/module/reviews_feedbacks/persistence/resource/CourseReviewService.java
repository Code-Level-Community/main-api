package com.codelevel.module.reviews_feedbacks.persistence.resource;

import com.codelevel.module.reviews_feedbacks.domain.Rating;
import com.codelevel.module.reviews_feedbacks.domain.ReviewComment;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.CourseReviewCreateRequest;
import com.codelevel.module.reviews_feedbacks.http.rest.dto.CourseReviewUpdateRequest;
import com.codelevel.module.reviews_feedbacks.infra.client.CourseRestClient;
import com.codelevel.module.reviews_feedbacks.persistence.entity.CourseReviewEntity;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CourseReviewService {

    private static final Logger log = Logger.getLogger(CourseReviewService.class);

    private final CourseRestClient courseRestClient;

    @Inject
    public CourseReviewService(@RestClient CourseRestClient courseRestClient) {
        this.courseRestClient = courseRestClient;
    }

    @Transactional
    public CourseReviewEntity create(CourseReviewCreateRequest request, UUID userId) {
        new Rating(request.rating());
        new ReviewComment(request.comment());

        validateCourseExists(request.courseId());

        CourseReviewEntity.findByUserAndCourse(userId, request.courseId()).ifPresent(existing -> {
            throw new ResourceAlreadyExists("You have already reviewed this course");
        });

        CourseReviewEntity entity = new CourseReviewEntity();
        entity.setUserId(userId);
        entity.setCourseId(request.courseId());
        entity.setRating(request.rating());
        entity.setPositive(request.isPositive());
        entity.setComment(request.comment());
        entity.persist();

        log.infof("CourseReview created: userId=%s, courseId=%d", userId, request.courseId());
        return entity;
    }

    @Transactional
    public CourseReviewEntity update(Long id, CourseReviewUpdateRequest request, UUID userId) {
        new Rating(request.rating());
        new ReviewComment(request.comment());

        CourseReviewEntity entity = findById(id);

        if (!entity.getUserId().equals(userId)) {
            throw new BusinessRuleException("You do not have permission to edit this review");
        }

        entity.setRating(request.rating());
        entity.setPositive(request.isPositive());
        entity.setComment(request.comment());
        entity.persist();

        return entity;
    }

    @Transactional
    public void delete(Long id, UUID userId, boolean isAdmin) {
        CourseReviewEntity entity = findById(id);

        if (!isAdmin && !entity.getUserId().equals(userId)) {
            throw new BusinessRuleException("You do not have permission to delete this review");
        }

        entity.delete();
        log.infof("CourseReview deleted: id=%d by userId=%s (isAdmin=%b)", id, userId, isAdmin);
    }

    public CourseReviewEntity findById(Long id) {
        return CourseReviewEntity.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Review not found"));
    }

    public List<CourseReviewEntity> findByCourse(Long courseId) {
        return CourseReviewEntity.findByCourseId(courseId);
    }

    public List<CourseReviewEntity> findByUser(UUID userId) {
        return CourseReviewEntity.findByUserId(userId);
    }

    private void validateCourseExists(Long courseId) {
        try {
            var response = courseRestClient.getCourse(courseId);
            if (response.getStatus() == 404) {
                throw new ResourceNotFound("Course not found");
            }
        } catch (ResourceNotFound e) {
            throw e;
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                throw new ResourceNotFound("Course not found");
            }
            throw new BusinessRuleException("Could not validate the course");
        } catch (Exception e) {
            throw new BusinessRuleException("Could not validate the course");
        }
    }
}
