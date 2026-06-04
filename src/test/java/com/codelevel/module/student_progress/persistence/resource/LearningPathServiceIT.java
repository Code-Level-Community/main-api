package com.codelevel.module.student_progress.persistence.resource;

import com.codelevel.module.student_progress.persistence.entity.LearningPathCourseEntity;
import com.codelevel.module.student_progress.persistence.entity.LearningPathEntity;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import com.codelevel.shared.exception.ResourceNotFound;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LearningPathServiceIT {

    @Inject
    LearningPathService learningPathService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private LearningPathEntity createPath(UUID creatorId) {
        return learningPathService.create(
                "Trilha de Java Completa", "Do básico ao avançado", null, "BEGINNER", null, creatorId);
    }

    @Test
    @TestTransaction
    void shouldCreateLearningPathUnpublished() {
        LearningPathEntity entity = createPath(uid(1));

        assertNotNull(entity.getId());
        assertEquals("Trilha de Java Completa", entity.getTitle());
        assertEquals(uid(1), entity.getCreatorId());
        assertFalse(entity.isPublished());
        assertEquals(0L, entity.getCoursesCount());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenTitleIsBlank() {
        assertThrows(BusinessRuleException.class,
                () -> learningPathService.create("", "desc", null, "BEGINNER", null, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenTitleIsTooShort() {
        assertThrows(BusinessRuleException.class,
                () -> learningPathService.create("AB", "desc", null, "BEGINNER", null, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldPublishLearningPathByCreator() {
        LearningPathEntity entity = createPath(uid(2));

        LearningPathEntity published = learningPathService.publish(entity.getId(), uid(2), false);

        assertTrue(published.isPublished());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenNonCreatorTriesToPublish() {
        LearningPathEntity entity = createPath(uid(3));

        assertThrows(BusinessRuleException.class,
                () -> learningPathService.publish(entity.getId(), uid(999), false));
    }

    @Test
    @TestTransaction
    void shouldAllowAdminToPublishAnyPath() {
        LearningPathEntity entity = createPath(uid(4));

        LearningPathEntity published = learningPathService.publish(entity.getId(), uid(999), true);

        assertTrue(published.isPublished());
    }

    @Test
    @TestTransaction
    void shouldAddCourseAndIncrementCount() {
        LearningPathEntity path = createPath(uid(5));

        LearningPathCourseEntity course = learningPathService.addCourse(path.getId(), 101L, 1L, "Fundamentos", uid(5), false);

        assertNotNull(course.getId());
        assertEquals(101L, course.getCourseId());
        assertEquals(1L, course.getOrderPosition());

        LearningPathEntity updated = learningPathService.getById(path.getId());
        assertEquals(1L, updated.getCoursesCount());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenAddingDuplicateCourse() {
        LearningPathEntity path = createPath(uid(6));
        learningPathService.addCourse(path.getId(), 201L, 1L, null, uid(6), false);

        assertThrows(ResourceAlreadyExists.class,
                () -> learningPathService.addCourse(path.getId(), 201L, 2L, null, uid(6), false));
    }

    @Test
    @TestTransaction
    void shouldRemoveCourseAndDecrementCount() {
        LearningPathEntity path = createPath(uid(7));
        learningPathService.addCourse(path.getId(), 301L, 1L, null, uid(7), false);
        learningPathService.addCourse(path.getId(), 302L, 2L, null, uid(7), false);

        learningPathService.removeCourse(path.getId(), 301L, uid(7), false);

        LearningPathEntity updated = learningPathService.getById(path.getId());
        assertEquals(1L, updated.getCoursesCount());
    }

    @Test
    @TestTransaction
    void shouldListCoursesOrderedByPosition() {
        LearningPathEntity path = createPath(uid(8));
        learningPathService.addCourse(path.getId(), 401L, 2L, null, uid(8), false);
        learningPathService.addCourse(path.getId(), 402L, 1L, null, uid(8), false);

        List<LearningPathCourseEntity> courses = learningPathService.listCourses(path.getId());

        assertEquals(2, courses.size());
        assertEquals(1L, courses.get(0).getOrderPosition());
        assertEquals(2L, courses.get(1).getOrderPosition());
    }

    @Test
    @TestTransaction
    void shouldThrowResourceNotFoundForNonexistentPath() {
        assertThrows(ResourceNotFound.class, () -> learningPathService.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldDeletePathByCreator() {
        LearningPathEntity path = createPath(uid(9));
        Long id = path.getId();

        learningPathService.delete(id, uid(9), false);

        assertThrows(ResourceNotFound.class, () -> learningPathService.getById(id));
    }
}
