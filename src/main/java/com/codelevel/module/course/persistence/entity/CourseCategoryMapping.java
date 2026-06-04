package com.codelevel.module.course.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.io.Serializable;

@Table(name = "CL_COURSE_CATEGORY_MAPPING")
@Entity
public class CourseCategoryMapping extends PanacheEntityBase {

    @EmbeddedId
    private ChaveComposta id;

    @Embeddable
    public static class ChaveComposta implements Serializable {

        @Column(name = "ccm_course_id")
        private Long courseId;

        @Column(name = "ccm_category_id")
        private Long categoryId;

        public ChaveComposta() {}

        public ChaveComposta(Long courseId, Long categoryId) {
            this.courseId = courseId;
            this.categoryId = categoryId;
        }

        public Long getCourseId() { return courseId; }
        public Long getCategoryId() { return categoryId; }
    }

    public CourseCategoryMapping() {}

    public CourseCategoryMapping(Long courseId, Long categoryId) {
        this.id = new ChaveComposta(courseId, categoryId);
    }

    public static void removeByCourseAndCategory(Long courseId, Long categoryId) {
        delete("id.courseId = ?1 and id.categoryId = ?2", courseId, categoryId);
    }

    public ChaveComposta getId() { return id; }
    public void setId(ChaveComposta id) { this.id = id; }
}
