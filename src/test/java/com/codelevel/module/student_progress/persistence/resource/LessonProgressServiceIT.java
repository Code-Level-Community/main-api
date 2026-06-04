package com.codelevel.module.student_progress.persistence.resource;

import com.codelevel.module.student_progress.persistence.entity.LessonProgressEntity;
import com.codelevel.shared.exception.ResourceNotFound;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LessonProgressServiceIT {

    @Inject
    LessonProgressService lessonProgressService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    @Test
    @TestTransaction
    void shouldTrackProgressWithoutCompletingBelow90Percent() {
        LessonProgressEntity entity = lessonProgressService.trackProgress(uid(1), 101L, 80L, 100L);

        assertNotNull(entity.getId());
        assertFalse(entity.isCompleted());
        assertEquals(80.0, entity.getCompletionPercentage());
        assertNull(entity.getCompletedAt());
    }

    @Test
    @TestTransaction
    void shouldAutoCompleteWhenWatchTimeReaches90Percent() {
        LessonProgressEntity entity = lessonProgressService.trackProgress(uid(1), 102L, 90L, 100L);

        assertTrue(entity.isCompleted());
        assertNotNull(entity.getCompletedAt());
        assertEquals(90.0, entity.getCompletionPercentage());
    }

    @Test
    @TestTransaction
    void shouldAutoCompleteWhenWatchTimeExceeds90Percent() {
        LessonProgressEntity entity = lessonProgressService.trackProgress(uid(1), 103L, 95L, 100L);

        assertTrue(entity.isCompleted());
    }

    @Test
    @TestTransaction
    void shouldMarkCompleteManually() {
        LessonProgressEntity entity = lessonProgressService.markComplete(uid(2), 201L);

        assertTrue(entity.isCompleted());
        assertEquals(100.0, entity.getCompletionPercentage());
        assertNotNull(entity.getCompletedAt());
    }

    @Test
    @TestTransaction
    void shouldUpsertProgressOnSubsequentTrack() {
        lessonProgressService.trackProgress(uid(3), 301L, 30L, 100L);
        LessonProgressEntity updated = lessonProgressService.trackProgress(uid(3), 301L, 60L, 100L);

        assertEquals(60.0, updated.getCompletionPercentage());
        assertFalse(updated.isCompleted());
    }

    @Test
    @TestTransaction
    void shouldNotOverwriteCompletionOnceSet() {
        lessonProgressService.trackProgress(uid(4), 401L, 91L, 100L);
        LessonProgressEntity stillComplete = lessonProgressService.trackProgress(uid(4), 401L, 50L, 100L);

        assertTrue(stillComplete.isCompleted());
    }

    @Test
    @TestTransaction
    void shouldGetByUserAndLesson() {
        lessonProgressService.trackProgress(uid(5), 501L, 45L, 100L);

        LessonProgressEntity found = lessonProgressService.getByUserAndLesson(uid(5), 501L);

        assertEquals(uid(5), found.getUserId());
        assertEquals(501L, found.getLessonId());
    }

    @Test
    @TestTransaction
    void shouldThrowResourceNotFoundForUntrackedLesson() {
        assertThrows(ResourceNotFound.class,
                () -> lessonProgressService.getByUserAndLesson(uid(999), 999999L));
    }

    @Test
    @TestTransaction
    void shouldListProgressByUserId() {
        lessonProgressService.trackProgress(uid(6), 601L, 20L, 100L);
        lessonProgressService.trackProgress(uid(6), 602L, 40L, 100L);

        var list = lessonProgressService.listByUserId(uid(6));

        assertTrue(list.size() >= 2);
        assertTrue(list.stream().allMatch(e -> e.getUserId().equals(uid(6))));
    }
}
