package com.codelevel.module.student_progress.persistence.resource;

import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.module.student_progress.domain.LearningPathTitle;
import com.codelevel.module.student_progress.persistence.entity.LearningPathCourseEntity;
import com.codelevel.module.student_progress.persistence.entity.LearningPathEntity;
import com.codelevel.module.student_progress.persistence.entity.enums.LearningTrackDifficultyLevel;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LearningPathService implements CreateUpdate {

    @Transactional
    public LearningPathEntity create(String title, String description, String thumbnailUrl,
                                     String difficultyLevel, String prerequisite, UUID creatorId) {
        var pathTitle = new LearningPathTitle(title);

        LearningPathEntity entity = new LearningPathEntity();
        entity.setCreatorId(creatorId);
        entity.setTitle(pathTitle.value());
        entity.setDescription(description);
        entity.setThumbnailUrl(thumbnailUrl);
        entity.setDifficultyLevel(LearningTrackDifficultyLevel.valueOf(difficultyLevel));
        entity.setPrerequisite(prerequisite);
        saveOrUpdate(entity);
        return entity;
    }

    public LearningPathEntity getById(Long id) {
        return LearningPathEntity.<LearningPathEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Learning path not found"));
    }

    public List<LearningPathEntity> listPublished() {
        return LearningPathEntity.findAllPublished();
    }

    public List<LearningPathEntity> listByCreator(UUID creatorId) {
        return LearningPathEntity.findByCreatorId(creatorId);
    }

    @Transactional
    public LearningPathEntity publish(Long id, UUID requesterId, boolean isAdmin) {
        LearningPathEntity entity = getById(id);

        if (!isAdmin && !entity.getCreatorId().equals(requesterId)) {
            throw new BusinessRuleException("You do not have permission to publish this learning path");
        }

        entity.setPublished(true);
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public LearningPathCourseEntity addCourse(Long learningPathId, Long courseId, Long orderPosition,
                                               String learningObjectives, UUID requesterId, boolean isAdmin) {
        LearningPathEntity path = getById(learningPathId);

        if (!isAdmin && !path.getCreatorId().equals(requesterId)) {
            throw new BusinessRuleException("You do not have permission to modify this learning path");
        }

        LearningPathCourseEntity.findByLearningPathAndCourse(learningPathId, courseId).ifPresent(c -> {
            throw new ResourceAlreadyExists("This course is already in this learning path");
        });

        LearningPathCourseEntity courseEntity = new LearningPathCourseEntity();
        courseEntity.setLearningPathId(learningPathId);
        courseEntity.setCourseId(courseId);
        courseEntity.setOrderPosition(orderPosition);
        courseEntity.setLearningObjectives(learningObjectives);
        saveOrUpdate(courseEntity);

        path.setCoursesCount(path.getCoursesCount() + 1);
        saveOrUpdate(path);

        return courseEntity;
    }

    @Transactional
    public void removeCourse(Long learningPathId, Long courseId, UUID requesterId, boolean isAdmin) {
        LearningPathEntity path = getById(learningPathId);

        if (!isAdmin && !path.getCreatorId().equals(requesterId)) {
            throw new BusinessRuleException("You do not have permission to modify this learning path");
        }

        LearningPathCourseEntity courseEntity = LearningPathCourseEntity.findByLearningPathAndCourse(learningPathId, courseId)
                .orElseThrow(() -> new ResourceNotFound("Course not found in this learning path"));

        courseEntity.delete();

        path.setCoursesCount(Math.max(0L, path.getCoursesCount() - 1));
        saveOrUpdate(path);
    }

    @Transactional
    public void delete(Long id, UUID requesterId, boolean isAdmin) {
        LearningPathEntity entity = getById(id);

        if (!isAdmin && !entity.getCreatorId().equals(requesterId)) {
            throw new BusinessRuleException("You do not have permission to delete this learning path");
        }

        entity.delete();
    }

    public List<LearningPathCourseEntity> listCourses(Long learningPathId) {
        getById(learningPathId);
        return LearningPathCourseEntity.findByLearningPathId(learningPathId);
    }
}
