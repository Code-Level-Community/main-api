package com.codelevel.module.gamification.persistence.entity;

import com.codelevel.module.gamification.persistence.entity.enums.SourceXPTransaction;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Table(
    name = "CL_XP_TRANSACTION",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_xp_transaction_user_source",
        columnNames = {"xpt_user_id", "xpt_source", "xpt_source_id"}
    )
)
@Entity
public class XPTransactionEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cl_xp_transaction_seq")
    @SequenceGenerator(name = "cl_xp_transaction_seq", sequenceName = "cl_xp_transaction_seq", allocationSize = 1)
    private Long id;

    @Column(name = "xpt_user_id", nullable = false)
    private UUID userId;

    @Column(name = "xpt_xp_amount", nullable = false)
    private Long xpAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "xpt_source", nullable = false)
    private SourceXPTransaction source;

    @Column(name = "xpt_source_id", nullable = false)
    private Long sourceId;

    @Column(name = "xpt_description", columnDefinition = "text")
    private String description;

    @Column(name = "xpt_created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public static List<XPTransactionEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public static Optional<XPTransactionEntity> findByUserSourceAndSourceId(
            UUID userId, SourceXPTransaction source, Long sourceId) {
        return find("userId = ?1 and source = ?2 and sourceId = ?3", userId, source, sourceId)
                .firstResultOptional();
    }

    public static long sumXpByUserId(UUID userId) {
        return XPTransactionEntity.<XPTransactionEntity>find("userId", userId)
                .stream()
                .mapToLong(XPTransactionEntity::getXpAmount)
                .sum();
    }

    public static long sumXpByUserIdToday(UUID userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        return XPTransactionEntity.<XPTransactionEntity>find(
                "userId = ?1 and xpAmount > 0 and createdAt >= ?2 and createdAt <= ?3",
                userId, startOfDay, endOfDay
        ).stream()
                .mapToLong(XPTransactionEntity::getXpAmount)
                .sum();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getXpAmount() { return xpAmount; }
    public void setXpAmount(Long xpAmount) { this.xpAmount = xpAmount; }

    public SourceXPTransaction getSource() { return source; }
    public void setSource(SourceXPTransaction source) { this.source = source; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
