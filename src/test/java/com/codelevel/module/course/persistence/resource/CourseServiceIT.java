package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.CourseCreateRequest;
import com.codelevel.module.course.http.rest.dto.CourseUpdateRequest;
import com.codelevel.module.course.http.rest.dto.LessonCreateRequest;
import com.codelevel.module.course.http.rest.dto.ModuleCreateRequest;
import com.codelevel.module.course.persistence.entity.CourseCategoryEntity;
import com.codelevel.module.course.persistence.entity.CourseEntity;
import com.codelevel.module.course.persistence.entity.CourseTagEntity;
import com.codelevel.module.course.persistence.entity.enums.StatusCourse;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceNotFound;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CourseServiceIT {

    @Inject
    CourseService courseService;

    @Inject
    ModuleService moduleService;

    @Inject
    LessonService lessonService;

    @Inject
    CategoryService categoryService;

    @Inject
    TagService tagService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private CourseCreateRequest validRequest() {
        return new CourseCreateRequest("Java Fundamentals", "Learn Java from scratch", "https://img.example.com/thumb.jpg", "BEGINNER");
    }

    private CourseEntity createCourseWithContent(UUID instructorId) {
        CourseEntity course = courseService.create(validRequest(), instructorId);
        var module = moduleService.create(course.getId(), new ModuleCreateRequest("Module 1", null, 1L));
        lessonService.create(module.getId(), new LessonCreateRequest("Lesson 1", null, "TEXT", null, null, null, 1L, null));
        return course;
    }

    @Test
    @TestTransaction
    void shouldCreateCourseWithValidData() {
        CourseEntity entity = courseService.create(validRequest(), uid(1));

        assertNotNull(entity.getId());
        assertEquals("Java Fundamentals", entity.getTitle());
        assertEquals("Learn Java from scratch", entity.getDescription());
        assertEquals(StatusCourse.DRAFT, entity.getStatus());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldRejectBlankTitle() {
        CourseCreateRequest request = new CourseCreateRequest("", "desc", "http://img.com/t.jpg", "BEGINNER");
        assertThrows(BusinessRuleException.class, () -> courseService.create(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldRejectTitleTooShort() {
        CourseCreateRequest request = new CourseCreateRequest("AB", "desc", "http://img.com/t.jpg", "BEGINNER");
        assertThrows(BusinessRuleException.class, () -> courseService.create(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldRejectBlankDescription() {
        CourseCreateRequest request = new CourseCreateRequest("Valid Title", "", "http://img.com/t.jpg", "BEGINNER");
        assertThrows(BusinessRuleException.class, () -> courseService.create(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldUpdateCourse() {
        CourseEntity created = courseService.create(validRequest(), uid(1));

        CourseUpdateRequest update = new CourseUpdateRequest("Updated Title", "Updated description here", "https://new.com/img.jpg", "ADVANCED");
        CourseEntity updated = courseService.update(created.getId(), update, uid(1), false);

        assertEquals("Updated Title", updated.getTitle());
        assertEquals("ADVANCED", updated.getDifficultyLevel().name());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenUpdatingOtherInstructorCourse() {
        CourseEntity created = courseService.create(validRequest(), uid(1));

        CourseUpdateRequest update = new CourseUpdateRequest("Updated Title", "Updated description here", "https://new.com/img.jpg", "ADVANCED");
        assertThrows(BusinessRuleException.class,
                () -> courseService.update(created.getId(), update, uid(99), false));
    }

    @Test
    @TestTransaction
    void shouldPublishCourse() {
        CourseEntity created = createCourseWithContent(uid(1));
        assertEquals(StatusCourse.DRAFT, created.getStatus());

        CourseEntity published = courseService.publish(created.getId(), uid(1), false);

        assertEquals(StatusCourse.EXPERIMENTAL, published.getStatus());
        assertNotNull(published.getPublishedAt());
    }

    @Test
    @TestTransaction
    void shouldPublishCourseAsAdminToExpertApproved() {
        CourseEntity created = createCourseWithContent(uid(1));

        CourseEntity published = courseService.publish(created.getId(), uid(1), true);

        assertEquals(StatusCourse.EXPERT_APPROVED, published.getStatus());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenPublishingCourseWithoutContent() {
        CourseEntity created = courseService.create(validRequest(), uid(1));

        assertThrows(BusinessRuleException.class,
                () -> courseService.publish(created.getId(), uid(1), true));
    }

    @Test
    @TestTransaction
    void shouldDeleteCourse() {
        CourseEntity created = courseService.create(validRequest(), uid(1));
        Long id = created.getId();

        courseService.delete(id, uid(1), false);

        assertThrows(ResourceNotFound.class, () -> courseService.getById(id));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenDeletingPublishedCourse() {
        CourseEntity created = createCourseWithContent(uid(1));
        courseService.publish(created.getId(), uid(1), true);

        assertThrows(BusinessRuleException.class,
                () -> courseService.delete(created.getId(), uid(1), true));
    }

    @Test
    @TestTransaction
    void shouldThrowResourceNotFoundForNonexistentCourse() {
        assertThrows(ResourceNotFound.class, () -> courseService.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldListAllEnabledCourses() {
        courseService.create(validRequest(), uid(1));
        courseService.create(new CourseCreateRequest("Spring Boot", "Spring course", "http://img.com/s.jpg", "INTERMEDIATE"), uid(1));

        List<CourseEntity> courses = courseService.listAll();

        assertTrue(courses.size() >= 2);
        assertTrue(courses.stream().noneMatch(c -> c.getStatus() == StatusCourse.ARCHIVED));
    }

    @Test
    @TestTransaction
    void shouldListCoursesByInstructor() {
        courseService.create(validRequest(), uid(42));
        courseService.create(new CourseCreateRequest("Course 2", "Another course", "http://img.com/c.jpg", "BEGINNER"), uid(99));

        List<CourseEntity> courses = courseService.listByInstructor(uid(42));

        assertFalse(courses.isEmpty());
        assertTrue(courses.stream().allMatch(c -> c.getInstructorId().equals(uid(42))));
    }

    @Test
    @TestTransaction
    void shouldAddAndRemoveCategoryToCourse() {
        CourseEntity course = courseService.create(validRequest(), uid(1));
        CourseCategoryEntity cat = categoryService.create(new com.codelevel.module.course.http.rest.dto.CategoryCreateRequest("Backend", "backend", null));

        courseService.addCategory(course.getId(), cat.getId());
        courseService.removeCategory(course.getId(), cat.getId());
    }

    @Test
    @TestTransaction
    void shouldAddAndRemoveTagToCourse() {
        CourseEntity course = courseService.create(validRequest(), uid(1));
        CourseTagEntity tag = tagService.create(new com.codelevel.module.course.http.rest.dto.TagCreateRequest("java", "java"));

        courseService.addTag(course.getId(), tag.getId());
        courseService.removeTag(course.getId(), tag.getId());
    }
}
