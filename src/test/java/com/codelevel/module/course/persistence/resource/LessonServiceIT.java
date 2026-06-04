package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.CourseCreateRequest;
import com.codelevel.module.course.http.rest.dto.LessonCreateRequest;
import com.codelevel.module.course.http.rest.dto.LessonUpdateRequest;
import com.codelevel.module.course.http.rest.dto.ModuleCreateRequest;
import com.codelevel.module.course.persistence.entity.CourseEntity;
import com.codelevel.module.course.persistence.entity.LessonEntity;
import com.codelevel.module.course.persistence.entity.ModuleEntity;
import com.codelevel.shared.exception.ResourceNotFound;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LessonServiceIT {

    @Inject
    LessonService lessonService;

    @Inject
    ModuleService moduleService;

    @Inject
    CourseService courseService;

    private ModuleEntity createModule() {
        CourseEntity course = courseService.create(
                new CourseCreateRequest("Course", "Description", "http://img.com/t.jpg", "BEGINNER"),
                UUID.randomUUID());
        return moduleService.create(course.getId(), new ModuleCreateRequest("Module", null, 1L));
    }

    @Test
    @TestTransaction
    void shouldCreateLesson() {
        ModuleEntity module = createModule();
        LessonCreateRequest request = new LessonCreateRequest("Intro to Java", "Basic concepts", "VIDEO",
                "https://video.com/intro", 600L, null, 1L, 50L);

        LessonEntity entity = lessonService.create(module.getId(), request);

        assertNotNull(entity.getId());
        assertEquals("Intro to Java", entity.getTitle());
        assertEquals(module.getId(), entity.getModuleId());
        assertEquals(50L, entity.getXpReward());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldUpdateLesson() {
        ModuleEntity module = createModule();
        LessonEntity created = lessonService.create(module.getId(),
                new LessonCreateRequest("Old", null, "TEXT", null, null, "content", 1L, 10L));

        LessonEntity updated = lessonService.update(created.getId(),
                new LessonUpdateRequest("New Title", null, "TEXT", null, null, "updated", 2L, 20L));

        assertEquals("New Title", updated.getTitle());
        assertEquals(2L, updated.getOrderPosition());
        assertEquals(20L, updated.getXpReward());
    }

    @Test
    @TestTransaction
    void shouldDeleteLesson() {
        ModuleEntity module = createModule();
        LessonEntity created = lessonService.create(module.getId(),
                new LessonCreateRequest("To Delete", null, null, null, null, null, 1L, null));
        Long id = created.getId();

        lessonService.delete(id);

        assertThrows(ResourceNotFound.class, () -> lessonService.getById(id));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenLessonNotFound() {
        assertThrows(ResourceNotFound.class, () -> lessonService.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldListLessonsByModuleOrderedByPosition() {
        ModuleEntity module = createModule();
        lessonService.create(module.getId(), new LessonCreateRequest("Lesson B", null, null, null, null, null, 2L, null));
        lessonService.create(module.getId(), new LessonCreateRequest("Lesson A", null, null, null, null, null, 1L, null));

        List<LessonEntity> lessons = lessonService.listByModule(module.getId());

        assertEquals(2, lessons.size());
        assertEquals("Lesson A", lessons.get(0).getTitle());
        assertEquals("Lesson B", lessons.get(1).getTitle());
    }
}
