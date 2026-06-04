package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.persistence.entity.CourseEntity;
import com.codelevel.module.course.persistence.entity.enums.StatusCourse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseServiceTryApproveTest {

    private CourseService service;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new CourseService());
        doNothing().when(service).saveOrUpdate(any());
    }

    private CourseEntity entityWith(StatusCourse status, Double threshold) {
        CourseEntity e = new CourseEntity();
        e.setStatus(status);
        e.setApprovalThreshold(threshold);
        return e;
    }

    // --- status guard (status != EXPERIMENTAL) ---

    @Test
    void shouldNotApproveWhenStatusIsDraft() {
        CourseEntity entity = entityWith(StatusCourse.DRAFT, 70.0);
        doReturn(entity).when(service).getById(1L);

        service.tryApprove(1L, 5L, 5L);

        assertEquals(StatusCourse.DRAFT, entity.getStatus());
        verify(service, never()).saveOrUpdate(any());
    }

    @Test
    void shouldNotApproveWhenStatusIsExpertApproved() {
        CourseEntity entity = entityWith(StatusCourse.EXPERT_APPROVED, 70.0);
        doReturn(entity).when(service).getById(1L);

        service.tryApprove(1L, 5L, 5L);

        assertEquals(StatusCourse.EXPERT_APPROVED, entity.getStatus());
        verify(service, never()).saveOrUpdate(any());
    }

    // --- totalCount < 5 boundary ---

    @Test
    void shouldNotApproveWhenTotalCountIsFour() {
        CourseEntity entity = entityWith(StatusCourse.EXPERIMENTAL, 70.0);
        doReturn(entity).when(service).getById(1L);

        service.tryApprove(1L, 4L, 4L); // 4 < 5

        assertEquals(StatusCourse.EXPERIMENTAL, entity.getStatus());
        verify(service, never()).saveOrUpdate(any());
    }

    @Test
    void shouldEnterApprovalLogicWhenTotalCountIsExactlyFive() {
        CourseEntity entity = entityWith(StatusCourse.EXPERIMENTAL, 50.0);
        doReturn(entity).when(service).getById(1L);

        service.tryApprove(1L, 3L, 5L); // 60% >= 50%

        assertEquals(StatusCourse.COMMUNITY_APPROVED, entity.getStatus());
    }

    // --- null threshold guard ---

    @Test
    void shouldNotApproveWhenThresholdIsNull() {
        CourseEntity entity = entityWith(StatusCourse.EXPERIMENTAL, null);
        doReturn(entity).when(service).getById(1L);

        service.tryApprove(1L, 5L, 5L);

        assertEquals(StatusCourse.EXPERIMENTAL, entity.getStatus());
        verify(service, never()).saveOrUpdate(any());
    }

    // --- approvalRate >= threshold boundary ---

    @Test
    void shouldNotApproveWhenApprovalRateIsBelowThreshold() {
        CourseEntity entity = entityWith(StatusCourse.EXPERIMENTAL, 70.0);
        doReturn(entity).when(service).getById(1L);

        service.tryApprove(1L, 6L, 10L); // 60.0% < 70.0%

        assertEquals(StatusCourse.EXPERIMENTAL, entity.getStatus());
        verify(service, never()).saveOrUpdate(any());
    }

    @Test
    void shouldApproveWhenApprovalRateMeetsThresholdExactly() {
        CourseEntity entity = entityWith(StatusCourse.EXPERIMENTAL, 70.0);
        doReturn(entity).when(service).getById(1L);

        service.tryApprove(1L, 7L, 10L); // 70.0% == 70.0%

        assertEquals(StatusCourse.COMMUNITY_APPROVED, entity.getStatus());
        verify(service).saveOrUpdate(entity);
    }

    @Test
    void shouldApproveWhenApprovalRateExceedsThreshold() {
        CourseEntity entity = entityWith(StatusCourse.EXPERIMENTAL, 70.0);
        doReturn(entity).when(service).getById(1L);

        service.tryApprove(1L, 8L, 10L); // 80.0% > 70.0%

        assertEquals(StatusCourse.COMMUNITY_APPROVED, entity.getStatus());
        verify(service).saveOrUpdate(entity);
    }

    // --- side effects on approval ---

    @Test
    void shouldStoreCalculatedAverageRatingOnApproval() {
        CourseEntity entity = entityWith(StatusCourse.EXPERIMENTAL, 50.0);
        doReturn(entity).when(service).getById(1L);

        service.tryApprove(1L, 8L, 10L); // 80.0%

        assertEquals(80.0, entity.getAverageRating());
    }
}
