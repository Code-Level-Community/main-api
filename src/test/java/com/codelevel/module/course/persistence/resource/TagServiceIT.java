package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.TagCreateRequest;
import com.codelevel.module.course.persistence.entity.CourseTagEntity;
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
class TagServiceIT {

    @Inject
    TagService tagService;

    @Test
    @TestTransaction
    void shouldCreateTag() {
        String slug = "java-" + UUID.randomUUID().toString().substring(0, 8);
        CourseTagEntity entity = tagService.create(new TagCreateRequest("Java", slug));

        assertNotNull(entity.getId());
        assertEquals("Java", entity.getName());
        assertEquals(slug, entity.getSlug());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowOnDuplicateSlug() {
        String slug = "spring-" + UUID.randomUUID().toString().substring(0, 8);
        tagService.create(new TagCreateRequest("Spring", slug));

        assertThrows(ResourceAlreadyExists.class, () ->
                tagService.create(new TagCreateRequest("Spring Boot", slug)));
    }

    @Test
    @TestTransaction
    void shouldGetTagById() {
        String slug = "kotlin-" + UUID.randomUUID().toString().substring(0, 8);
        CourseTagEntity created = tagService.create(new TagCreateRequest("Kotlin", slug));

        CourseTagEntity found = tagService.getById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Kotlin", found.getName());
    }

    @Test
    void shouldThrowWhenTagNotFound() {
        assertThrows(ResourceNotFound.class, () -> tagService.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldListAllTags() {
        String slug1 = "tag1-" + UUID.randomUUID().toString().substring(0, 8);
        String slug2 = "tag2-" + UUID.randomUUID().toString().substring(0, 8);
        tagService.create(new TagCreateRequest("Tag One", slug1));
        tagService.create(new TagCreateRequest("Tag Two", slug2));

        List<CourseTagEntity> list = tagService.listAll();

        assertTrue(list.size() >= 2);
    }
}
