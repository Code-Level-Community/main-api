package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.CategoryCreateRequest;
import com.codelevel.module.course.persistence.entity.CourseCategoryEntity;
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
class CategoryServiceIT {

    @Inject
    CategoryService categoryService;

    @Test
    @TestTransaction
    void shouldCreateCategory() {
        String slug = "backend-" + UUID.randomUUID().toString().substring(0, 8);
        CourseCategoryEntity entity = categoryService.create(new CategoryCreateRequest("Backend", slug, "https://icon.com/b.svg"));

        assertNotNull(entity.getId());
        assertEquals("Backend", entity.getName());
        assertEquals(slug, entity.getSlug());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldThrowOnDuplicateSlug() {
        String slug = "unique-" + UUID.randomUUID().toString().substring(0, 8);
        categoryService.create(new CategoryCreateRequest("First", slug, null));

        assertThrows(ResourceAlreadyExists.class, () ->
                categoryService.create(new CategoryCreateRequest("Second", slug, null)));
    }

    @Test
    @TestTransaction
    void shouldGetCategoryById() {
        String slug = "frontend-" + UUID.randomUUID().toString().substring(0, 8);
        CourseCategoryEntity created = categoryService.create(new CategoryCreateRequest("Frontend", slug, null));

        CourseCategoryEntity found = categoryService.getById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Frontend", found.getName());
    }

    @Test
    void shouldThrowWhenCategoryNotFound() {
        assertThrows(ResourceNotFound.class, () -> categoryService.getById(999999L));
    }

    @Test
    @TestTransaction
    void shouldListAllCategories() {
        String slug1 = "cat1-" + UUID.randomUUID().toString().substring(0, 8);
        String slug2 = "cat2-" + UUID.randomUUID().toString().substring(0, 8);
        categoryService.create(new CategoryCreateRequest("Cat One", slug1, null));
        categoryService.create(new CategoryCreateRequest("Cat Two", slug2, null));

        List<CourseCategoryEntity> list = categoryService.listAll();

        assertTrue(list.size() >= 2);
    }
}
