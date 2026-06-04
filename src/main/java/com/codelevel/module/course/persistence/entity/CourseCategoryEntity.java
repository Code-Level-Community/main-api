package com.codelevel.module.course.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "CL_COURSE_CATEGORY")
@Entity
public class CourseCategoryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_course_category_seq", sequenceName = "cl_course_category_seq", allocationSize = 1)
    private Long id;

    @Column(name = "ca_name", nullable = false)
    private String name;

    @Column(name = "ca_slug", unique = true, nullable = false)
    private String slug;

    @Column(name = "ca_icon_url")
    private String iconUrl;

    @Column(name = "ca_created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public static CourseCategoryEntity findBySlug(String slug) {
        return find("slug", slug).firstResult();
    }

    public static List<CourseCategoryEntity> listAllOrdered() {
        return find("order by name asc").list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
