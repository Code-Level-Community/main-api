package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.CourseCreateRequest;
import com.codelevel.module.course.http.rest.dto.ModuleCreateRequest;
import com.codelevel.module.course.http.rest.dto.ModuleUpdateRequest;
import com.codelevel.module.course.persistence.entity.CourseEntity;
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
class ModuleServiceIT {

    @Inject
    ModuleService moduleService;

    @Inject
    CourseService courseService;

    private CourseEntity createCourse() {
        return courseService.create(
                new CourseCreateRequest("Test Course", "Description for test", "http://img.com/t.jpg", "BEGINNER"),
                UUID.randomUUID());
    }

    @Test
    @TestTransaction
    void shouldCreateModule() {
        CourseEntity course = createCourse();
        ModuleCreateRequest request = new ModuleCreateRequest("Module 1", "First module", 1L);

        ModuleEntity entity = moduleService.create(course.getId(), request);

        assertNotNull(entity.getId());
        assertEquals("Module 1", entity.getTitle());
        assertEquals(course.getId(), entity.getCourseId());
        assertEquals(1L, entity.getOrderPosition());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldUpdateModule() {
        CourseEntity course = createCourse();
        ModuleEntity created = moduleService.create(course.getId(), new ModuleCreateRequest("Old Title", "Old desc", 1L));

        ModuleEntity updated = moduleService.update(created.getId(), new ModuleUpdateRequest("New Title", "New desc", 2L));

        assertEquals("New Title", updated.getTitle());
        assertEquals(2L, updated.getOrderPosition());
    }

    @Test
    @TestTransaction
    void shouldDeleteModule() {
        CourseEntity course = createCourse();
        ModuleEntity created = moduleService.create(course.getId(), new ModuleCreateRequest("To Delete", null, 1L));
        Long id = created.getId();

        moduleService.delete(id);

        assertThrows(ResourceNotFound.class, () -> moduleService.getById(id));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenModuleNotFound() {
        assertThrows(ResourceNotFound.class, () -> moduleService.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldListModulesByCourseOrderedByPosition() {
        CourseEntity course = createCourse();
        moduleService.create(course.getId(), new ModuleCreateRequest("Module B", null, 2L));
        moduleService.create(course.getId(), new ModuleCreateRequest("Module A", null, 1L));
        moduleService.create(course.getId(), new ModuleCreateRequest("Module C", null, 3L));

        List<ModuleEntity> modules = moduleService.listByCourse(course.getId());

        assertEquals(3, modules.size());
        assertEquals("Module A", modules.get(0).getTitle());
        assertEquals("Module B", modules.get(1).getTitle());
        assertEquals("Module C", modules.get(2).getTitle());
    }
}
