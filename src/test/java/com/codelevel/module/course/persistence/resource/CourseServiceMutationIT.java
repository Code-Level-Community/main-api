package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.CourseCreateRequest;
import com.codelevel.module.course.http.rest.dto.LessonCreateRequest;
import com.codelevel.module.course.http.rest.dto.ModuleCreateRequest;
import com.codelevel.module.course.persistence.entity.CourseEntity;
import com.codelevel.module.course.persistence.entity.enums.StatusCourse;
import com.codelevel.shared.exception.BusinessRuleException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CourseServiceMutationIT {

    @Inject CourseService courseService;
    @Inject ModuleService moduleService;
    @Inject LessonService lessonService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private CourseEntity createExperimentalCourseWithThreshold(UUID instructorId, double threshold) {
        CourseEntity course = courseService.create(
                new CourseCreateRequest("Java Fundamentals", "Learn Java from scratch", "https://img.com/t.jpg", "BEGINNER"),
                instructorId
        );
        var module = moduleService.create(course.getId(), new ModuleCreateRequest("Module 1", null, 1L));
        lessonService.create(module.getId(), new LessonCreateRequest("Lesson 1", null, "TEXT", null, null, null, 1L, null));
        courseService.publish(course.getId(), instructorId, false);

        CourseEntity entity = courseService.getById(course.getId());
        entity.setApprovalThreshold(threshold);
        entity.persistAndFlush();
        return entity;
    }

    // --- tryApprove: status guard ---

    @Test
    @TestTransaction
    void shouldNotApproveWhenCourseIsNotExperimental() {
        CourseEntity course = courseService.create(
                new CourseCreateRequest("Java", "Desc", "https://img.com/t.jpg", "BEGINNER"), uid(1));

        courseService.tryApprove(course.getId(), 5L, 5L);

        assertEquals(StatusCourse.DRAFT, courseService.getById(course.getId()).getStatus());
    }

    // --- tryApprove: totalCount boundary (< 5) ---

    @Test
    @TestTransaction
    void shouldNotApproveWhenTotalCountIsBelowFive() {
        CourseEntity course = createExperimentalCourseWithThreshold(uid(1), 50.0);

        courseService.tryApprove(course.getId(), 4L, 4L);

        assertEquals(StatusCourse.EXPERIMENTAL, courseService.getById(course.getId()).getStatus());
    }

    @Test
    @TestTransaction
    void shouldEnterApprovalLogicWhenTotalCountIsExactlyFive() {
        CourseEntity course = createExperimentalCourseWithThreshold(uid(1), 50.0);

        // 3 positive / 5 total = 60% >= 50% threshold → should approve
        courseService.tryApprove(course.getId(), 3L, 5L);

        assertEquals(StatusCourse.COMMUNITY_APPROVED, courseService.getById(course.getId()).getStatus());
    }

    // --- tryApprove: approvalRate boundary (>= threshold) ---

    @Test
    @TestTransaction
    void shouldNotApproveWhenApprovalRateIsBelowThreshold() {
        CourseEntity course = createExperimentalCourseWithThreshold(uid(1), 70.0);

        // 6 / 10 = 60% < 70% threshold
        courseService.tryApprove(course.getId(), 6L, 10L);

        assertEquals(StatusCourse.EXPERIMENTAL, courseService.getById(course.getId()).getStatus());
    }

    @Test
    @TestTransaction
    void shouldApproveWhenApprovalRateMeetsThresholdExactly() {
        CourseEntity course = createExperimentalCourseWithThreshold(uid(1), 70.0);

        // 7 / 10 = 70.0% == 70.0% threshold (boundary >=)
        courseService.tryApprove(course.getId(), 7L, 10L);

        assertEquals(StatusCourse.COMMUNITY_APPROVED, courseService.getById(course.getId()).getStatus());
    }

    @Test
    @TestTransaction
    void shouldApproveWhenApprovalRateExceedsThreshold() {
        CourseEntity course = createExperimentalCourseWithThreshold(uid(1), 70.0);

        // 8 / 10 = 80% > 70%
        courseService.tryApprove(course.getId(), 8L, 10L);

        assertEquals(StatusCourse.COMMUNITY_APPROVED, courseService.getById(course.getId()).getStatus());
    }

    // --- tryApprove: null threshold guard ---

    @Test
    @TestTransaction
    void shouldNotApproveWhenThresholdIsNull() {
        CourseEntity course = courseService.create(
                new CourseCreateRequest("Java", "Desc", "https://img.com/t.jpg", "BEGINNER"), uid(1));
        var module = moduleService.create(course.getId(), new ModuleCreateRequest("Module 1", null, 1L));
        lessonService.create(module.getId(), new LessonCreateRequest("Lesson 1", null, "TEXT", null, null, null, 1L, null));
        courseService.publish(course.getId(), uid(1), false);
        // approvalThreshold is null by default

        courseService.tryApprove(course.getId(), 5L, 5L);

        assertEquals(StatusCourse.EXPERIMENTAL, courseService.getById(course.getId()).getStatus());
    }

    // --- tryApprove: side effects on approval ---

    @Test
    @TestTransaction
    void shouldStoreCalculatedAverageRatingOnApproval() {
        CourseEntity course = createExperimentalCourseWithThreshold(uid(1), 50.0);

        courseService.tryApprove(course.getId(), 8L, 10L); // 80%

        assertEquals(80.0, courseService.getById(course.getId()).getAverageRating());
    }

    // --- publish: status guard ---

    @Test
    @TestTransaction
    void shouldThrowWhenPublishingAlreadyPublishedCourse() {
        CourseEntity course = createExperimentalCourseWithThreshold(uid(1), 50.0);

        assertThrows(BusinessRuleException.class,
                () -> courseService.publish(course.getId(), uid(1), false));
    }

    // --- publish: authorization guard ---

    @Test
    @TestTransaction
    void shouldThrowWhenNonOwnerNonAdminTriesToPublish() {
        CourseEntity course = courseService.create(
                new CourseCreateRequest("Java", "Desc", "https://img.com/t.jpg", "BEGINNER"), uid(1));
        var module = moduleService.create(course.getId(), new ModuleCreateRequest("Module 1", null, 1L));
        lessonService.create(module.getId(), new LessonCreateRequest("Lesson 1", null, "TEXT", null, null, null, 1L, null));

        assertThrows(BusinessRuleException.class,
                () -> courseService.publish(course.getId(), uid(99), false));
    }

    @Test
    @TestTransaction
    void shouldAllowAdminToPublishCourseOwnedByOther() {
        CourseEntity course = courseService.create(
                new CourseCreateRequest("Java", "Desc", "https://img.com/t.jpg", "BEGINNER"), uid(1));
        var module = moduleService.create(course.getId(), new ModuleCreateRequest("Module 1", null, 1L));
        lessonService.create(module.getId(), new LessonCreateRequest("Lesson 1", null, "TEXT", null, null, null, 1L, null));

        CourseEntity published = courseService.publish(course.getId(), uid(99), true);

        assertEquals(StatusCourse.EXPERT_APPROVED, published.getStatus());
    }

    // --- delete: status guard ---

    @Test
    @TestTransaction
    void shouldThrowWhenDeletingNonDraftCourseAsAdmin() {
        CourseEntity course = createExperimentalCourseWithThreshold(uid(1), 50.0);

        assertThrows(BusinessRuleException.class,
                () -> courseService.delete(course.getId(), uid(99), true));
    }

    // --- delete: authorization guard ---

    @Test
    @TestTransaction
    void shouldThrowWhenNonOwnerNonAdminTriesToDelete() {
        CourseEntity course = courseService.create(
                new CourseCreateRequest("Java", "Desc", "https://img.com/t.jpg", "BEGINNER"), uid(1));

        assertThrows(BusinessRuleException.class,
                () -> courseService.delete(course.getId(), uid(99), false));
    }

    @Test
    @TestTransaction
    void shouldAllowAdminToDeleteDraftCourseOwnedByOther() {
        CourseEntity course = courseService.create(
                new CourseCreateRequest("Java", "Desc", "https://img.com/t.jpg", "BEGINNER"), uid(1));

        assertDoesNotThrow(() -> courseService.delete(course.getId(), uid(99), true));
    }
}
