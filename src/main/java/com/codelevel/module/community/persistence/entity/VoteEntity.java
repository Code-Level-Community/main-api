package com.codelevel.module.community.persistence.entity;

import com.codelevel.module.community.persistence.entity.enums.VotableType;
import com.codelevel.module.community.persistence.entity.enums.VoteType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Table(
    name = "CL_VOTE",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_vote_user_votable",
        columnNames = {"v_user_id", "v_votable_type", "v_votable_id"}
    )
)
@Entity
public class VoteEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "cl_vote_seq", allocationSize = 1)
    private Long id;

    @Column(name = "v_user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "v_votable_type", nullable = false)
    private VotableType votableType;

    @Column(name = "v_votable_id", nullable = false)
    private Long votableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "v_vote_type", nullable = false)
    private VoteType voteType;

    @Column(name = "v_created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public static Optional<VoteEntity> findByUserAndVotable(UUID userId, VotableType votableType, Long votableId) {
        return find("userId = ?1 and votableType = ?2 and votableId = ?3", userId, votableType, votableId)
                .firstResultOptional();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public VotableType getVotableType() { return votableType; }
    public void setVotableType(VotableType votableType) { this.votableType = votableType; }

    public Long getVotableId() { return votableId; }
    public void setVotableId(Long votableId) { this.votableId = votableId; }

    public VoteType getVoteType() { return voteType; }
    public void setVoteType(VoteType voteType) { this.voteType = voteType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
