package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.persistence.entity.CourseEntity;
import com.codelevel.module.course.persistence.entity.LessonEntity;
import com.codelevel.module.course.persistence.entity.ModuleEntity;
import com.codelevel.module.course.persistence.entity.enums.StatusCourse;
import com.codelevel.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseServicePublishTest {

    private CourseService service;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new CourseService());
        doNothing().when(service).saveOrUpdate(any());
    }

    private CourseEntity draftCourseOwnedBy(UUID instructorId) {
        CourseEntity e = new CourseEntity();
        e.setStatus(StatusCourse.DRAFT);
        e.setInstructorId(instructorId);
        return e;
    }

    private ModuleEntity moduleWithId(Long id) {
        ModuleEntity m = new ModuleEntity();
        m.setId(id);
        return m;
    }

    private LessonEntity lesson() {
        return new LessonEntity();
    }

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000099");

    // --- status guard (status != DRAFT) ---

    @Test
    void shouldThrowWhenPublishingAlreadyExperimentalCourse() {
        CourseEntity entity = draftCourseOwnedBy(OWNER);
        entity.setStatus(StatusCourse.EXPERIMENTAL);
        doReturn(entity).when(service).getById(1L);

        assertThrows(BusinessRuleException.class,
                () -> service.publish(1L, OWNER, false));
    }

    @Test
    void shouldThrowWhenPublishingArchivedCourse() {
        CourseEntity entity = draftCourseOwnedBy(OWNER);
        entity.setStatus(StatusCourse.ARCHIVED);
        doReturn(entity).when(service).getById(1L);

        assertThrows(BusinessRuleException.class,
                () -> service.publish(1L, OWNER, false));
    }

    // --- authorization guard (!isAdmin && !owner) ---

    @Test
    void shouldThrowWhenNonAdminNonOwnerTriesToPublish() {
        CourseEntity entity = draftCourseOwnedBy(OWNER);
        doReturn(entity).when(service).getById(1L);

        // No ModuleEntity mock needed — auth check fires before content check
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.publish(1L, OTHER, false));
        assertTrue(ex.getMessage().contains("permission"),
                "Expected auth error but got: " + ex.getMessage());
    }

    @Test
    void shouldAllowAdminToPublishEvenIfNotOwner() {
        CourseEntity entity = draftCourseOwnedBy(OWNER);
        doReturn(entity).when(service).getById(1L);
        ModuleEntity module = moduleWithId(10L);

        try (MockedStatic<ModuleEntity> modules = mockStatic(ModuleEntity.class);
             MockedStatic<LessonEntity> lessons = mockStatic(LessonEntity.class)) {

            modules.when(() -> ModuleEntity.findByCourseId(1L)).thenReturn(List.of(module));
            lessons.when(() -> LessonEntity.findByModuleId(10L)).thenReturn(List.of(lesson()));

            CourseEntity result = service.publish(1L, OTHER, true);

            assertEquals(StatusCourse.EXPERT_APPROVED, result.getStatus());
            verify(service).saveOrUpdate(result);
        }
    }

    // --- content guard (!hasContent) ---

    @Test
    void shouldThrowWhenCourseHasNoModules() {
        CourseEntity entity = draftCourseOwnedBy(OWNER);
        doReturn(entity).when(service).getById(1L);

        try (MockedStatic<ModuleEntity> modules = mockStatic(ModuleEntity.class)) {
            modules.when(() -> ModuleEntity.findByCourseId(1L)).thenReturn(List.of());

            assertThrows(BusinessRuleException.class,
                    () -> service.publish(1L, OWNER, false));
        }
    }

    @Test
    void shouldThrowWhenAllModulesHaveNoLessons() {
        CourseEntity entity = draftCourseOwnedBy(OWNER);
        doReturn(entity).when(service).getById(1L);
        ModuleEntity module = moduleWithId(10L);

        try (MockedStatic<ModuleEntity> modules = mockStatic(ModuleEntity.class);
             MockedStatic<LessonEntity> lessons = mockStatic(LessonEntity.class)) {

            modules.when(() -> ModuleEntity.findByCourseId(1L)).thenReturn(List.of(module));
            lessons.when(() -> LessonEntity.findByModuleId(10L)).thenReturn(List.of());

            assertThrows(BusinessRuleException.class,
                    () -> service.publish(1L, OWNER, false));
        }
    }

    // --- status assignment (isAdmin ? EXPERT_APPROVED : EXPERIMENTAL) ---

    @Test
    void shouldPublishAsExperimentalWhenNonAdmin() {
        CourseEntity entity = draftCourseOwnedBy(OWNER);
        doReturn(entity).when(service).getById(1L);
        ModuleEntity module = moduleWithId(10L);

        try (MockedStatic<ModuleEntity> modules = mockStatic(ModuleEntity.class);
             MockedStatic<LessonEntity> lessons = mockStatic(LessonEntity.class)) {

            modules.when(() -> ModuleEntity.findByCourseId(1L)).thenReturn(List.of(module));
            lessons.when(() -> LessonEntity.findByModuleId(10L)).thenReturn(List.of(lesson()));

            CourseEntity result = service.publish(1L, OWNER, false);

            assertEquals(StatusCourse.EXPERIMENTAL, result.getStatus());
            assertNotNull(result.getPublishedAt());
            verify(service).saveOrUpdate(result);
        }
    }

    @Test
    void shouldPublishAsExpertApprovedWhenAdmin() {
        CourseEntity entity = draftCourseOwnedBy(OWNER);
        doReturn(entity).when(service).getById(1L);
        ModuleEntity module = moduleWithId(10L);

        try (MockedStatic<ModuleEntity> modules = mockStatic(ModuleEntity.class);
             MockedStatic<LessonEntity> lessons = mockStatic(LessonEntity.class)) {

            modules.when(() -> ModuleEntity.findByCourseId(1L)).thenReturn(List.of(module));
            lessons.when(() -> LessonEntity.findByModuleId(10L)).thenReturn(List.of(lesson()));

            CourseEntity result = service.publish(1L, OWNER, true);

            assertEquals(StatusCourse.EXPERT_APPROVED, result.getStatus());
        }
    }
}
