package com.codelevel.module.course.persistence.entity;

import com.codelevel.module.course.persistence.entity.enums.ExerciseType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "CL_EXERCISE")
@Entity
public class ExerciseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_exercise_seq", sequenceName = "cl_exercise_seq", allocationSize = 1)
    private Long id;

    @Column(name = "e_lesson_id")
    private Long lessonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "e_type", nullable = false)
    private ExerciseType type;

    @Column(name = "e_title", nullable = false)
    private String title;

    @Column(name = "e_description", columnDefinition = "text")
    private String description;

    @Column(name = "e_quiz_data", columnDefinition = "text")
    private String quizData;

    @Column(name = "e_code_template", columnDefinition = "text")
    private String codeTemplate;

    @Column(name = "e_test_cases", columnDefinition = "text")
    private String testCases;

    @Column(name = "e_max_attempts")
    private Long maxAttempts;

    @Column(name = "e_xp_reward")
    private Long xpReward;

    @Column(name = "e_created_at")
    private LocalDateTime createdAt;

    @Column(name = "e_updated_at")
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

    public static List<ExerciseEntity> findByLessonId(Long lessonId) {
        return find("lessonId", lessonId).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public ExerciseType getType() { return type; }
    public void setType(ExerciseType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getQuizData() { return quizData; }
    public void setQuizData(String quizData) { this.quizData = quizData; }

    public String getCodeTemplate() { return codeTemplate; }
    public void setCodeTemplate(String codeTemplate) { this.codeTemplate = codeTemplate; }

    public String getTestCases() { return testCases; }
    public void setTestCases(String testCases) { this.testCases = testCases; }

    public Long getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Long maxAttempts) { this.maxAttempts = maxAttempts; }

    public Long getXpReward() { return xpReward; }
    public void setXpReward(Long xpReward) { this.xpReward = xpReward; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
