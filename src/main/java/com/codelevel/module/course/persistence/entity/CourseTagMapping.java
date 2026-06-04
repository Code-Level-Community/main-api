package com.codelevel.module.course.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.io.Serializable;

@Table(name = "CL_COURSE_TAG_MAPPING")
@Entity
public class CourseTagMapping extends PanacheEntityBase {

    @EmbeddedId
    private ChaveComposta id;

    @Embeddable
    public static class ChaveComposta implements Serializable {

        @Column(name = "ctm_course_id")
        private Long courseId;

        @Column(name = "ctm_tag_id")
        private Long tagId;

        public ChaveComposta() {}

        public ChaveComposta(Long courseId, Long tagId) {
            this.courseId = courseId;
            this.tagId = tagId;
        }

        public Long getCourseId() { return courseId; }
        public Long getTagId() { return tagId; }
    }

    public CourseTagMapping() {}

    public CourseTagMapping(Long courseId, Long tagId) {
        this.id = new ChaveComposta(courseId, tagId);
    }

    public static void removeByCourseAndTag(Long courseId, Long tagId) {
        delete("id.courseId = ?1 and id.tagId = ?2", courseId, tagId);
    }

    public ChaveComposta getId() { return id; }
    public void setId(ChaveComposta id) { this.id = id; }
}
