package com.codelevel.module.student_progress.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Table(
    name = "CL_LEARNING_PATH_COURSE",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_learning_path_course",
        columnNames = {"lpc_learning_path_id", "lpc_course_id"}
    )
)
@Entity
public class LearningPathCourseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_learning_path_course_seq", allocationSize = 1)
    private Long id;

    @Column(name = "lpc_learning_path_id", nullable = false)
    private Long learningPathId;

    @Column(name = "lpc_course_id", nullable = false)
    private Long courseId;

    @Column(name = "lpc_order_position")
    private Long orderPosition;

    @Column(name = "lpc_learning_objectives", columnDefinition = "text")
    private String learningObjectives;

    @Column(name = "lpc_added_at", nullable = false)
    private LocalDateTime addedAt;

    @PrePersist
    public void prePersist() {
        addedAt = LocalDateTime.now();
    }

    public static List<LearningPathCourseEntity> findByLearningPathId(Long learningPathId) {
        return find("learningPathId = ?1 order by orderPosition asc", learningPathId).list();
    }

    public static Optional<LearningPathCourseEntity> findByLearningPathAndCourse(Long learningPathId, Long courseId) {
        return find("learningPathId = ?1 and courseId = ?2", learningPathId, courseId).firstResultOptional();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLearningPathId() { return learningPathId; }
    public void setLearningPathId(Long learningPathId) { this.learningPathId = learningPathId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getOrderPosition() { return orderPosition; }
    public void setOrderPosition(Long orderPosition) { this.orderPosition = orderPosition; }

    public String getLearningObjectives() { return learningObjectives; }
    public void setLearningObjectives(String learningObjectives) { this.learningObjectives = learningObjectives; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}
