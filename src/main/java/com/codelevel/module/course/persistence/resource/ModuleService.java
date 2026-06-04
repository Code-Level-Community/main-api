package com.codelevel.module.course.persistence.resource;

import com.codelevel.module.course.http.rest.dto.ModuleCreateRequest;
import com.codelevel.module.course.http.rest.dto.ModuleUpdateRequest;
import com.codelevel.module.course.persistence.entity.ModuleEntity;
import com.codelevel.module.identity.persistence.resource.CreateUpdate;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ModuleService implements CreateUpdate {

    @Transactional
    public ModuleEntity create(Long courseId, ModuleCreateRequest request) {
        ModuleEntity entity = new ModuleEntity();
        entity.setCourseId(courseId);
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setOrderPosition(request.orderPosition());
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public ModuleEntity update(Long id, ModuleUpdateRequest request) {
        ModuleEntity entity = getById(id);
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setOrderPosition(request.orderPosition());
        saveOrUpdate(entity);
        return entity;
    }

    @Transactional
    public void delete(Long id) {
        ModuleEntity entity = getById(id);
        entity.delete();
    }

    public ModuleEntity getById(Long id) {
        ModuleEntity entity = ModuleEntity.findById(id);
        if (Objects.isNull(entity)) throw new ResourceNotFound("Module not found");
        return entity;
    }

    public List<ModuleEntity> listByCourse(Long courseId) {
        return ModuleEntity.findByCourseId(courseId);
    }
}
