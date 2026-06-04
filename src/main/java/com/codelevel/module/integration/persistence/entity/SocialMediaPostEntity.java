package com.codelevel.module.integration.persistence.entity;

import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPlatform;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPostStatus;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPostType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "CL_SOCIAL_MEDIA_POST")
public class SocialMediaPostEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cl_social_media_post_seq")
    @SequenceGenerator(name = "cl_social_media_post_seq", sequenceName = "cl_social_media_post_seq", allocationSize = 1)
    private Long id;

    @Column(name = "smp_user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "smp_platform", nullable = false)
    private SocialMediaPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "smp_post_type", nullable = false)
    private SocialMediaPostType postType;

    @Column(name = "smp_reference_id")
    private Long referenceId;

    @Column(name = "smp_content", columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "smp_media_url", columnDefinition = "text")
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "smp_status", nullable = false)
    private SocialMediaPostStatus status;

    @Column(name = "smp_error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "smp_scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "smp_posted_at")
    private LocalDateTime postedAt;

    @Column(name = "smp_created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = SocialMediaPostStatus.PENDING;
    }

    public static Optional<SocialMediaPostEntity> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public static List<SocialMediaPostEntity> findByPlatform(SocialMediaPlatform platform) {
        return find("platform", platform).list();
    }

    public static List<SocialMediaPostEntity> findByStatus(SocialMediaPostStatus status) {
        return find("status", status).list();
    }

    public static List<SocialMediaPostEntity> findByPostType(SocialMediaPostType postType) {
        return find("postType", postType).list();
    }

    public static List<SocialMediaPostEntity> findByUserId(UUID userId) {
        return find("userId", userId).list();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public SocialMediaPlatform getPlatform() { return platform; }
    public void setPlatform(SocialMediaPlatform platform) { this.platform = platform; }

    public SocialMediaPostType getPostType() { return postType; }
    public void setPostType(SocialMediaPostType postType) { this.postType = postType; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public SocialMediaPostStatus getStatus() { return status; }
    public void setStatus(SocialMediaPostStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}