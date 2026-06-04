package com.codelevel.module.integration.persistence.resource;

import com.codelevel.module.integration.http.rest.dto.CreateSocialMediaPostRequest;
import com.codelevel.module.integration.persistence.entity.SocialMediaPostEntity;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPlatform;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPostStatus;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPostType;
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
class SocialMediaPostServiceIT {

    @Inject
    SocialMediaPostService service;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private CreateSocialMediaPostRequest validRequest() {
        return new CreateSocialMediaPostRequest(
                "INSTAGRAM",
                "NEW_COURSE",
                1L,
                "Novo curso de Quarkus com GraalVM disponível! Aprenda a construir aplicações nativas de alta performance.",
                null,
                null
        );
    }

    @Test
    @TestTransaction
    void shouldCreatePostWithValidData() {
        SocialMediaPostEntity entity = service.createPost(validRequest(), uid(1));

        assertNotNull(entity.getId());
        assertEquals(uid(1), entity.getUserId());
        assertEquals(SocialMediaPlatform.INSTAGRAM, entity.getPlatform());
        assertEquals(SocialMediaPostType.NEW_COURSE, entity.getPostType());
        assertEquals(SocialMediaPostStatus.PENDING, entity.getStatus());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenContentIsBlank() {
        var request = new CreateSocialMediaPostRequest("INSTAGRAM", "NEW_COURSE", null, "", null, null);
        assertThrows(BusinessRuleException.class, () -> service.createPost(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenContentExceedsMaxLength() {
        var request = new CreateSocialMediaPostRequest("INSTAGRAM", "NEW_COURSE", null, "a".repeat(2201), null, null);
        assertThrows(BusinessRuleException.class, () -> service.createPost(request, uid(1)));
    }

    @Test
    @TestTransaction
    void shouldPublishPendingPost() {
        SocialMediaPostEntity created = service.createPost(validRequest(), uid(1));
        assertEquals(SocialMediaPostStatus.PENDING, created.getStatus());

        SocialMediaPostEntity published = service.publishPost(created.getId());

        assertEquals(SocialMediaPostStatus.POSTED, published.getStatus());
        assertNotNull(published.getPostedAt());
        assertNull(published.getErrorMessage());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenPublishingAlreadyPostedPost() {
        SocialMediaPostEntity entity = service.createPost(validRequest(), uid(1));
        service.publishPost(entity.getId());

        assertThrows(BusinessRuleException.class, () -> service.publishPost(entity.getId()));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenRetryingNonFailedPost() {
        SocialMediaPostEntity entity = service.createPost(validRequest(), uid(1));

        assertThrows(BusinessRuleException.class, () -> service.retryPost(entity.getId()));
    }

    @Test
    @TestTransaction
    void shouldCancelPendingPost() {
        SocialMediaPostEntity entity = service.createPost(validRequest(), uid(1));
        Long id = entity.getId();

        service.cancelPost(id);

        assertThrows(ResourceNotFound.class, () -> service.getById(id));
    }

    @Test
    @TestTransaction
    void shouldThrowWhenCancelingPublishedPost() {
        SocialMediaPostEntity entity = service.createPost(validRequest(), uid(1));
        service.publishPost(entity.getId());

        assertThrows(BusinessRuleException.class, () -> service.cancelPost(entity.getId()));
    }

    @Test
    @TestTransaction
    void shouldGetById() {
        SocialMediaPostEntity created = service.createPost(validRequest(), uid(1));

        SocialMediaPostEntity found = service.getById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals(SocialMediaPlatform.INSTAGRAM, found.getPlatform());
    }

    @Test
    @TestTransaction
    void shouldThrowWhenPostNotFound() {
        assertThrows(ResourceNotFound.class, () -> service.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldListAll() {
        service.createPost(validRequest(), uid(1));
        service.createPost(new CreateSocialMediaPostRequest("LINKEDIN", "TOP_CONTRIBUTOR", 2L,
                "Parabéns ao nosso top contributor do mês!", null, null), uid(1));

        List<SocialMediaPostEntity> all = service.listAll();

        assertTrue(all.size() >= 2);
    }

    @Test
    @TestTransaction
    void shouldListByPlatform() {
        service.createPost(validRequest(), uid(1));
        service.createPost(new CreateSocialMediaPostRequest("LINKEDIN", "TOP_CONTRIBUTOR", 2L,
                "Parabéns ao nosso top contributor do mês!", null, null), uid(1));

        List<SocialMediaPostEntity> instagramPosts = service.listByPlatform(SocialMediaPlatform.INSTAGRAM);

        assertTrue(instagramPosts.stream().allMatch(p -> p.getPlatform() == SocialMediaPlatform.INSTAGRAM));
    }

    @Test
    @TestTransaction
    void shouldListByStatus() {
        service.createPost(validRequest(), uid(1));

        List<SocialMediaPostEntity> pending = service.listByStatus(SocialMediaPostStatus.PENDING);

        assertTrue(pending.stream().allMatch(p -> p.getStatus() == SocialMediaPostStatus.PENDING));
    }

    @Test
    @TestTransaction
    void shouldListByType() {
        service.createPost(validRequest(), uid(1));

        List<SocialMediaPostEntity> newCoursePosts = service.listByType(SocialMediaPostType.NEW_COURSE);

        assertTrue(newCoursePosts.stream().allMatch(p -> p.getPostType() == SocialMediaPostType.NEW_COURSE));
    }
}
