package com.codelevel.module.student_progress.persistence.resource;

import com.codelevel.module.student_progress.persistence.entity.CourseEnrollmentEntity;
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
class EnrollmentServiceIT {

    @Inject
    EnrollmentService enrollmentService;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    @Test
    @TestTransaction
    void shouldEnrollUserInCourse() {
        CourseEnrollmentEntity entity = enrollmentService.enroll(10L, uid(1));

        assertNotNull(entity.getId());
        assertEquals(uid(1), entity.getUserId());
        assertEquals(10L, entity.getCourseId());
        assertNotNull(entity.getEnrolledAt());
        assertEquals(0.0, entity.getProgressPercentage());
        assertEquals(0L, entity.getLessonsCompleted());
        assertEquals(0L, entity.getTotalStudyTimeMinutes());
        assertNull(entity.getStartedAt());
        assertNull(entity.getCompletedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenAlreadyEnrolled() {
        enrollmentService.enroll(20L, uid(2));

        assertThrows(ResourceAlreadyExists.class, () -> enrollmentService.enroll(20L, uid(2)));
    }

    @Test
    @TestTransaction
    void shouldGetEnrollmentById() {
        CourseEnrollmentEntity enrolled = enrollmentService.enroll(30L, uid(3));

        CourseEnrollmentEntity found = enrollmentService.getById(enrolled.getId());

        assertEquals(enrolled.getId(), found.getId());
        assertEquals(uid(3), found.getUserId());
        assertEquals(30L, found.getCourseId());
    }

    @Test
    @TestTransaction
    void shouldThrowResourceNotFoundForNonexistentEnrollment() {
        assertThrows(ResourceNotFound.class, () -> enrollmentService.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldListEnrollmentsByUserId() {
        enrollmentService.enroll(41L, uid(4));
        enrollmentService.enroll(42L, uid(4));

        List<CourseEnrollmentEntity> list = enrollmentService.listByUserId(uid(4));

        assertTrue(list.size() >= 2);
        assertTrue(list.stream().allMatch(e -> e.getUserId().equals(uid(4))));
    }

    @Test
    @TestTransaction
    void shouldListEnrollmentsByCourseId() {
        enrollmentService.enroll(50L, uid(5));
        enrollmentService.enroll(50L, uid(6));

        List<CourseEnrollmentEntity> list = enrollmentService.listByCourseId(50L);

        assertTrue(list.size() >= 2);
        assertTrue(list.stream().allMatch(e -> e.getCourseId().equals(50L)));
    }

    @Test
    @TestTransaction
    void shouldUpdateProgressAndSetStartedAt() {
        CourseEnrollmentEntity entity = enrollmentService.enroll(60L, uid(7));

        CourseEnrollmentEntity updated = enrollmentService.updateProgress(entity.getId(), 5L, 10L, 30L);

        assertNotNull(updated.getStartedAt());
        assertEquals(5L, updated.getLessonsCompleted());
        assertEquals(50.0, updated.getProgressPercentage());
        assertEquals(30L, updated.getTotalStudyTimeMinutes());
        assertNull(updated.getCompletedAt());
    }

    @Test
    @TestTransaction
    void shouldSetCompletedAtWhenProgressReaches100() {
        CourseEnrollmentEntity entity = enrollmentService.enroll(70L, uid(8));

        CourseEnrollmentEntity updated = enrollmentService.updateProgress(entity.getId(), 10L, 10L, 60L);

        assertEquals(100.0, updated.getProgressPercentage());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    @TestTransaction
    void shouldNotOverwriteStartedAtOnSubsequentUpdates() {
        CourseEnrollmentEntity entity = enrollmentService.enroll(80L, uid(9));
        enrollmentService.updateProgress(entity.getId(), 1L, 10L, 10L);
        CourseEnrollmentEntity firstUpdate = enrollmentService.getById(entity.getId());

        enrollmentService.updateProgress(entity.getId(), 2L, 10L, 10L);
        CourseEnrollmentEntity secondUpdate = enrollmentService.getById(entity.getId());

        assertNotNull(firstUpdate.getStartedAt());
        assertEquals(firstUpdate.getStartedAt(), secondUpdate.getStartedAt());
    }
}
