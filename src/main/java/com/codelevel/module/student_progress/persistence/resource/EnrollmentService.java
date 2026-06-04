package com.codelevel.module.student_progress.persistence.resource;

import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.module.student_progress.persistence.entity.CourseEnrollmentEntity;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EnrollmentService implements CreateUpdate {

    private static final Logger log = Logger.getLogger(EnrollmentService.class);

    @Transactional
    public CourseEnrollmentEntity enroll(Long courseId, UUID userId) {
        CourseEnrollmentEntity.findByUserAndCourse(userId, courseId).ifPresent(e -> {
            throw new ResourceAlreadyExists("You are already enrolled in this course");
        });

        CourseEnrollmentEntity entity = new CourseEnrollmentEntity();
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        saveOrUpdate(entity);
        log.infof("Enrollment created: userId=%s, courseId=%d", userId, courseId);
        return entity;
    }

    public CourseEnrollmentEntity getById(Long id) {
        return CourseEnrollmentEntity.<CourseEnrollmentEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Enrollment not found"));
    }

    public List<CourseEnrollmentEntity> listByUserId(UUID userId) {
        return CourseEnrollmentEntity.findByUserId(userId);
    }

    public List<CourseEnrollmentEntity> listByCourseId(Long courseId) {
        return CourseEnrollmentEntity.findByCourseId(courseId);
    }

    public CourseEnrollmentEntity findByUserAndCourse(UUID userId, Long courseId) {
        return CourseEnrollmentEntity.findByUserAndCourse(userId, courseId)
                .orElseThrow(() -> new ResourceNotFound("Enrollment not found for this user and course"));
    }

    @Transactional
    public CourseEnrollmentEntity updateProgress(Long enrollmentId, Long lessonsCompleted, Long totalLessons, Long additionalStudyMinutes) {
        CourseEnrollmentEntity entity = getById(enrollmentId);

        if (entity.getStartedAt() == null) {
            entity.setStartedAt(LocalDateTime.now());
        }

        entity.setLessonsCompleted(lessonsCompleted);
        entity.setTotalStudyTimeMinutes(entity.getTotalStudyTimeMinutes() + additionalStudyMinutes);

        double percentage = totalLessons > 0 ? (double) lessonsCompleted / totalLessons * 100.0 : 0.0;
        entity.setProgressPercentage(Math.min(100.0, percentage));

        if (entity.getProgressPercentage() >= 100.0 && entity.getCompletedAt() == null) {
            entity.setCompletedAt(LocalDateTime.now());
        }

        saveOrUpdate(entity);
        return entity;
    }
}