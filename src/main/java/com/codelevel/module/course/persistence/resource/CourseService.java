package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.domain.CourseDescription;
import com.codelevel.module.course.domain.CourseTitle;
import com.codelevel.module.course.http.rest.dto.CourseCreateRequest;
import com.codelevel.module.course.http.rest.dto.CourseUpdateRequest;
import com.codelevel.module.course.persistence.entity.CourseCategoryMapping;
import com.codelevel.module.course.persistence.entity.CourseEntity;
import com.codelevel.module.course.persistence.entity.CourseTagMapping;
import com.codelevel.module.course.persistence.entity.LessonEntity;
import com.codelevel.module.course.persistence.entity.ModuleEntity;
import com.codelevel.module.course.persistence.entity.enums.DifficultyCourse;
import com.codelevel.module.course.persistence.entity.enums.StatusCourse;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CourseService implements CreateUpdate {

    private static final Logger log = Logger.getLogger(CourseService.class);

    @Transactional
    public CourseEntity create(CourseCreateRequest request, UUID instructorId) {
        var title = new CourseTitle(request.title());
        var description = new CourseDescription(request.description());

        CourseEntity entity = new CourseEntity();
        entity.setTitle(title.value());
        entity.setDescription(description.value());
        entity.setThumbnailUrl(request.thumbnailUrl());
        entity.setDifficultyLevel(DifficultyCourse.valueOf(request.difficultyLevel()));
        entity.setInstructorId(instructorId);
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public CourseEntity update(Long id, CourseUpdateRequest request, UUID requesterId, boolean isAdmin) {
        CourseEntity entity = getById(id);

        if (!isAdmin && !entity.getInstructorId().equals(requesterId)) {
            throw new BusinessRuleException("You do not have permission to edit this course");
        }

        var title = new CourseTitle(request.title());
        var description = new CourseDescription(request.description());

        entity.setTitle(title.value());
        entity.setDescription(description.value());
        entity.setThumbnailUrl(request.thumbnailUrl());
        entity.setDifficultyLevel(DifficultyCourse.valueOf(request.difficultyLevel()));
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public CourseEntity publish(Long id, UUID requesterId, boolean isAdmin) {
        CourseEntity entity = getById(id);

        if (entity.getStatus() != StatusCourse.DRAFT) {
            throw new BusinessRuleException("Only DRAFT courses can be published");
        }

        if (!isAdmin && !entity.getInstructorId().equals(requesterId)) {
            throw new BusinessRuleException("You do not have permission to publish this course");
        }

        boolean hasContent = ModuleEntity.findByCourseId(id).stream()
                .anyMatch(module -> !LessonEntity.findByModuleId(module.getId()).isEmpty());

        if (!hasContent) {
            throw new BusinessRuleException("The course must have at least 1 module with 1 lesson before publishing");
        }

        entity.setStatus(isAdmin ? StatusCourse.EXPERT_APPROVED : StatusCourse.EXPERIMENTAL);
        entity.setPublishedAt(LocalDateTime.now());
        saveOrUpdate(entity);
        log.infof("Course published: courseId=%d, status=%s, requesterId=%s", id, entity.getStatus(), requesterId);
        return entity;
    }

    @Transactional
    public CourseEntity archive(Long id) {
        CourseEntity entity = getById(id);
        entity.setStatus(StatusCourse.ARCHIVED);
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public void delete(Long id, UUID requesterId, boolean isAdmin) {
        CourseEntity entity = getById(id);

        if (entity.getStatus() != StatusCourse.DRAFT) {
            throw new BusinessRuleException("Only DRAFT courses can be deleted. Use the archive endpoint for published courses.");
        }

        if (!isAdmin && !entity.getInstructorId().equals(requesterId)) {
            throw new BusinessRuleException("You do not have permission to delete this course");
        }

        log.infof("Course deleted: courseId=%d, requesterId=%s", id, requesterId);
        entity.delete();
    }

    @Transactional
    public void tryApprove(Long courseId, long positiveCount, long totalCount) {
        CourseEntity entity = getById(courseId);

        if (entity.getStatus() != StatusCourse.EXPERIMENTAL) return;
        if (totalCount < 5) return;

        Double threshold = entity.getApprovalThreshold();
        if (threshold == null) return;

        double approvalRate = ((double) positiveCount / totalCount) * 100;
        if (approvalRate >= threshold) {
            entity.setStatus(StatusCourse.COMMUNITY_APPROVED);
            entity.setAverageRating(approvalRate);
            saveOrUpdate(entity);
        }
    }

    public CourseEntity getById(Long id) {
        return CourseEntity.<CourseEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Course not found"));
    }

    public List<CourseEntity> listAll() {
        return CourseEntity.findAllEnabled();
    }

    public List<CourseEntity> listByInstructor(UUID instructorId) {
        return CourseEntity.findByInstructorId(instructorId);
    }

    @Transactional
    public void addCategory(Long courseId, Long categoryId) {
        getById(courseId);
        CourseCategoryMapping mapping = new CourseCategoryMapping(courseId, categoryId);
        saveOrUpdate(mapping);
    }

    @Transactional
    public void removeCategory(Long courseId, Long categoryId) {
        CourseCategoryMapping.removeByCourseAndCategory(courseId, categoryId);
    }

    @Transactional
    public void addTag(Long courseId, Long tagId) {
        getById(courseId);
        CourseTagMapping mapping = new CourseTagMapping(courseId, tagId);
        saveOrUpdate(mapping);
    }

    @Transactional
    public void removeTag(Long courseId, Long tagId) {
        CourseTagMapping.removeByCourseAndTag(courseId, tagId);
    }
}
