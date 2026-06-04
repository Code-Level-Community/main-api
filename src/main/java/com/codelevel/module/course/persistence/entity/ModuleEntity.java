package com.codelevel.module.course.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "CL_MODULE")
@Entity
public class ModuleEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_module_seq", sequenceName = "cl_module_seq", allocationSize = 1)
    private Long id;

    @Column(name = "m_course_id")
    private Long courseId;

    @Column(name = "m_title", nullable = false)
    private String title;

    @Column(name = "m_description", columnDefinition = "text")
    private String description;

    @Column(name = "m_order_position")
    private Long orderPosition;

    @Column(name = "m_created_at")
    private LocalDateTime createdAt;

    @Column(name = "m_updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static List<ModuleEntity> findByCourseId(Long courseId) {
        return find("courseId = ?1 order by orderPosition asc nulls last", courseId).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getOrderPosition() { return orderPosition; }
    public void setOrderPosition(Long orderPosition) { this.orderPosition = orderPosition; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
