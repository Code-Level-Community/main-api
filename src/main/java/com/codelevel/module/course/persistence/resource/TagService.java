package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.TagCreateRequest;
import com.codelevel.module.course.persistence.entity.CourseTagEntity;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class TagService implements CreateUpdate {

    @Transactional
    public CourseTagEntity create(TagCreateRequest request) {
        CourseTagEntity entity = new CourseTagEntity();
        entity.setName(request.name());
        entity.setSlug(request.slug());
        saveOrUpdate(entity);
        return entity;
    }

    public CourseTagEntity getById(Long id) {
        CourseTagEntity entity = CourseTagEntity.findById(id);
        if (Objects.isNull(entity)) throw new ResourceNotFound("Tag not found");
        return entity;
    }

    public List<CourseTagEntity> listAll() {
        return CourseTagEntity.listAllOrdered();
    }
}
