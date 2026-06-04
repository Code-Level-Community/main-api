package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.CategoryCreateRequest;
import com.codelevel.module.course.persistence.entity.CourseCategoryEntity;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class CategoryService implements CreateUpdate {

    @Transactional
    public CourseCategoryEntity create(CategoryCreateRequest request) {
        CourseCategoryEntity entity = new CourseCategoryEntity();
        entity.setName(request.name());
        entity.setSlug(request.slug());
        entity.setIconUrl(request.iconUrl());
        saveOrUpdate(entity);
        return entity;
    }

    public CourseCategoryEntity getById(Long id) {
        CourseCategoryEntity entity = CourseCategoryEntity.findById(id);
        if (Objects.isNull(entity)) throw new ResourceNotFound("Category not found");
        return entity;
    }

    public List<CourseCategoryEntity> listAll() {
        return CourseCategoryEntity.listAllOrdered();
    }
}
