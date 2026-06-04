package com.codelevel.module.course.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "CL_COURSE_TAG")
@Entity
public class CourseTagEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_course_tag_seq", sequenceName = "cl_course_tag_seq", allocationSize = 1)
    private Long id;

    @Column(name = "ct_name", nullable = false)
    private String name;

    @Column(name = "ct_slug", unique = true, nullable = false)
    private String slug;

    @Column(name = "ct_created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public static CourseTagEntity findBySlug(String slug) {
        return find("slug", slug).firstResult();
    }

    public static List<CourseTagEntity> listAllOrdered() {
        return find("order by name asc").list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
