package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.*;
import com.codelevel.module.course.persistence.entity.CourseEntity;
import com.codelevel.module.course.persistence.entity.ExerciseEntity;
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
class ExerciseServiceIT {

    @Inject
    ExerciseService exerciseService;

    @Inject
    LessonService lessonService;

    @Inject
    ModuleService moduleService;

    @Inject
    CourseService courseService;

    private LessonEntity createLesson() {
        CourseEntity course = courseService.create(
                new CourseCreateRequest("Course", "Desc", "http://img.com/t.jpg", "BEGINNER"),
                UUID.randomUUID());
        ModuleEntity module = moduleService.create(course.getId(), new ModuleCreateRequest("Module", null, 1L));
        return lessonService.create(module.getId(), new LessonCreateRequest("Lesson", null, null, null, null, null, 1L, null));
    }

    @Test
    @TestTransaction
    void shouldCreateExercise() {
        LessonEntity lesson = createLesson();
        ExerciseCreateRequest request = new ExerciseCreateRequest(
                "QUIZ", "Quiz 1", "First quiz", "{\"question\":\"What is Java?\"}", null, null, 3L, 30L);

        ExerciseEntity entity = exerciseService.create(lesson.getId(), request);

        assertNotNull(entity.getId());
        assertEquals("Quiz 1", entity.getTitle());
        assertEquals(lesson.getId(), entity.getLessonId());
        assertEquals(3L, entity.getMaxAttempts());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldUpdateExercise() {
        LessonEntity lesson = createLesson();
        ExerciseEntity created = exerciseService.create(lesson.getId(),
                new ExerciseCreateRequest("QUIZ", "Old", null, null, null, null, 2L, 10L));

        ExerciseEntity updated = exerciseService.update(created.getId(),
                new ExerciseUpdateRequest("QUIZ", "New Title", "New desc", null, null, null, 5L, 50L));

        assertEquals("New Title", updated.getTitle());
        assertEquals(5L, updated.getMaxAttempts());
        assertEquals(50L, updated.getXpReward());
    }

    @Test
    @TestTransaction
    void shouldDeleteExercise() {
        LessonEntity lesson = createLesson();
        ExerciseEntity created = exerciseService.create(lesson.getId(),
                new ExerciseCreateRequest("QUIZ", "To Delete", null, null, null, null, null, null));
        Long id = created.getId();

        exerciseService.delete(id);

        assertThrows(ResourceNotFound.class, () -> exerciseService.getById(id));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenExerciseNotFound() {
        assertThrows(ResourceNotFound.class, () -> exerciseService.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldListExercisesByLesson() {
        LessonEntity lesson = createLesson();
        exerciseService.create(lesson.getId(), new ExerciseCreateRequest("QUIZ", "Ex 1", null, null, null, null, null, null));
        exerciseService.create(lesson.getId(), new ExerciseCreateRequest("CODE_CHALLENGE", "Ex 2", null, null, null, null, null, null));

        List<ExerciseEntity> exercises = exerciseService.listByLesson(lesson.getId());

        assertEquals(2, exercises.size());
    }
}