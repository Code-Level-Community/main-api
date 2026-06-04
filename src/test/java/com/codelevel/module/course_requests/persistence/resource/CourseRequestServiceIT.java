package com.codelevel.module.course_requests.persistence.resource;

import com.codelevel.module.course_requests.http.rest.dto.CourseRequestCreateRequest;
import com.codelevel.module.course_requests.persistence.entity.CourseRequestEntity;
import com.codelevel.module.course_requests.persistence.entity.enums.CourseRequestVoteType;
import com.codelevel.module.course_requests.persistence.entity.enums.StatusCourseRequest;
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
class CourseRequestServiceIT {

    @Inject
    CourseRequestService service;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private CourseRequestCreateRequest validRequest() {
        return new CourseRequestCreateRequest(
                "Curso de Kotlin do zero ao avançado",
                "Gostaria de ver um curso completo sobre Kotlin com foco em backend e Android.",
                1L
        );
    }

    @Test
    @TestTransaction
    void shouldCreateCourseRequestWithValidData() {
        CourseRequestEntity entity = service.create(validRequest(), uid(1));

        assertNotNull(entity.getId());
        assertEquals("Curso de Kotlin do zero ao avançado", entity.getTitle());
        assertEquals(StatusCourseRequest.PENDING, entity.getStatus());
        assertEquals(0L, entity.getUpvotes());
        assertEquals(0L, entity.getDownvotes());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenTitleIsBlank() {
        var request = new CourseRequestCreateRequest("", "Descrição válida para o curso solicitado.", 1L);
        assertThrows(BusinessRuleException.class, () -> service.create(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenTitleIsTooShort() {
        var request = new CourseRequestCreateRequest("AB", "Descrição válida para o curso solicitado.", 1L);
        assertThrows(BusinessRuleException.class, () -> service.create(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenDescriptionIsBlank() {
        var request = new CourseRequestCreateRequest("Título válido do curso", "", 1L);
        assertThrows(BusinessRuleException.class, () -> service.create(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldUpvotePendingRequest() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));

        CourseRequestEntity voted = service.vote(created.getId(), uid(2), CourseRequestVoteType.UPVOTE);

        assertEquals(1L, voted.getUpvotes());
        assertEquals(0L, voted.getDownvotes());
    }

    @Test
    @TestTransaction
    void shouldDownvotePendingRequest() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));

        CourseRequestEntity voted = service.vote(created.getId(), uid(2), CourseRequestVoteType.DOWNVOTE);

        assertEquals(0L, voted.getUpvotes());
        assertEquals(1L, voted.getDownvotes());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenVotingTwice() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));
        service.vote(created.getId(), uid(2), CourseRequestVoteType.UPVOTE);

        assertThrows(ResourceAlreadyExists.class,
                () -> service.vote(created.getId(), uid(2), CourseRequestVoteType.UPVOTE));
    }

    @Test
    @TestTransaction
    void shouldAutoApproveWhenThresholdReached() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));

        for (int i = 2; i <= 21; i++) {
            service.vote(created.getId(), uid(i), CourseRequestVoteType.UPVOTE);
        }

        CourseRequestEntity result = service.getById(created.getId());
        assertEquals(StatusCourseRequest.APPROVED, result.getStatus());
        assertNotNull(result.getApprovedAt());
    }

    @Test
    @TestTransaction
    void shouldNotAutoApproveWhenBelowThreshold() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));

        for (int i = 2; i <= 16; i++) {
            if (i <= 6) {
                service.vote(created.getId(), uid(i), CourseRequestVoteType.DOWNVOTE);
            } else {
                service.vote(created.getId(), uid(i), CourseRequestVoteType.UPVOTE);
            }
        }

        CourseRequestEntity result = service.getById(created.getId());
        assertEquals(StatusCourseRequest.PENDING, result.getStatus());
    }

    @Test
    @TestTransaction
    void shouldRejectPendingRequest() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));

        CourseRequestEntity rejected = service.reject(created.getId());

        assertEquals(StatusCourseRequest.REJECTED, rejected.getStatus());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenRejectingNonPendingRequest() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));
        service.reject(created.getId());

        assertThrows(BusinessRuleException.class, () -> service.reject(created.getId()));
    }

    @Test
    @TestTransaction
    void shouldAcceptApprovedRequest() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));

        for (int i = 2; i <= 21; i++) {
            service.vote(created.getId(), uid(i), CourseRequestVoteType.UPVOTE);
        }

        CourseRequestEntity accepted = service.accept(created.getId(), uid(99));

        assertEquals(StatusCourseRequest.IN_PROGRESS, accepted.getStatus());
        assertEquals(uid(99), accepted.getAssignedInstructorId());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenAcceptingNonApprovedRequest() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));

        assertThrows(BusinessRuleException.class, () -> service.accept(created.getId(), uid(99)));
    }

    @Test
    @TestTransaction
    void shouldGiveUpInProgressRequest() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));
        for (int i = 2; i <= 21; i++) {
            service.vote(created.getId(), uid(i), CourseRequestVoteType.UPVOTE);
        }
        service.accept(created.getId(), uid(99));

        CourseRequestEntity gaveUp = service.giveUp(created.getId(), uid(99));

        assertEquals(StatusCourseRequest.APPROVED, gaveUp.getStatus());
        assertNull(gaveUp.getAssignedInstructorId());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenGivingUpWithWrongInstructor() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));
        for (int i = 2; i <= 21; i++) {
            service.vote(created.getId(), uid(i), CourseRequestVoteType.UPVOTE);
        }
        service.accept(created.getId(), uid(99));

        assertThrows(BusinessRuleException.class, () -> service.giveUp(created.getId(), uid(77)));
    }

    @Test
    @TestTransaction
    void shouldCompleteInProgressRequest() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));
        for (int i = 2; i <= 21; i++) {
            service.vote(created.getId(), uid(i), CourseRequestVoteType.UPVOTE);
        }
        service.accept(created.getId(), uid(99));

        CourseRequestEntity completed = service.complete(created.getId(), uid(99), 500L);

        assertEquals(StatusCourseRequest.COMPLETED, completed.getStatus());
        assertEquals(500L, completed.getCreatedCourseId());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenCompletingWithWrongInstructor() {
        CourseRequestEntity created = service.create(validRequest(), uid(1));
        for (int i = 2; i <= 21; i++) {
            service.vote(created.getId(), uid(i), CourseRequestVoteType.UPVOTE);
        }
        service.accept(created.getId(), uid(99));

        assertThrows(BusinessRuleException.class, () -> service.complete(created.getId(), uid(77), 500L));
    }

    @Test
    @TestTransaction
    void shouldThrowResourceNotFoundForNonexistentRequest() {
        assertThrows(ResourceNotFound.class, () -> service.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldListAllRequests() {
        service.create(validRequest(), uid(1));
        service.create(new CourseRequestCreateRequest("Outro curso de Python", "Descrição sobre Python para análise de dados.", null), uid(2));

        List<CourseRequestEntity> list = service.listAll();

        assertTrue(list.size() >= 2);
    }

    @Test
    @TestTransaction
    void shouldListRequestsByStatus() {
        service.create(validRequest(), uid(1));
        CourseRequestEntity second = service.create(
                new CourseRequestCreateRequest("Curso de Python", "Descrição válida do curso de Python avançado.", null), uid(2));
        service.reject(second.getId());

        List<CourseRequestEntity> pending = service.listByStatus(StatusCourseRequest.PENDING);
        List<CourseRequestEntity> rejected = service.listByStatus(StatusCourseRequest.REJECTED);

        assertTrue(pending.stream().allMatch(r -> r.getStatus() == StatusCourseRequest.PENDING));
        assertTrue(rejected.stream().allMatch(r -> r.getStatus() == StatusCourseRequest.REJECTED));
    }
}
