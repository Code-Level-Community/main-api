package com.codelevel.module.integration.persistence.resource;

import com.codelevel.module.integration.domain.PostContent;
import com.codelevel.module.integration.http.rest.dto.CreateSocialMediaPostRequest;
import com.codelevel.module.integration.infra.publisher.SocialMediaPublisher;
import com.codelevel.module.integration.infra.publisher.SocialMediaPublisherFactory;
import com.codelevel.module.integration.persistence.entity.SocialMediaPostEntity;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPlatform;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPostStatus;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPostType;
import com.codelevel.shared.exception.BusinessRuleException;
import com.codelevel.shared.exception.ResourceNotFound;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SocialMediaPostService {

    private final SocialMediaPublisherFactory publisherFactory;

    @Inject
    public SocialMediaPostService(SocialMediaPublisherFactory publisherFactory) {
        this.publisherFactory = publisherFactory;
    }

    @Transactional
    public SocialMediaPostEntity createPost(CreateSocialMediaPostRequest request, UUID userId) {
        var content = new PostContent(request.content());

        SocialMediaPostEntity entity = new SocialMediaPostEntity();
        entity.setUserId(userId);
        entity.setPlatform(SocialMediaPlatform.valueOf(request.platform()));
        entity.setPostType(SocialMediaPostType.valueOf(request.postType()));
        entity.setReferenceId(request.referenceId());
        entity.setContent(content.value());
        entity.setMediaUrl(request.mediaUrl());
        entity.setScheduledAt(request.scheduledAt());
        entity.persistAndFlush();
        return entity;
    }

    @Transactional
    public SocialMediaPostEntity publishPost(Long postId) {
        SocialMediaPostEntity post = getById(postId);

        if (post.getStatus() == SocialMediaPostStatus.POSTED) {
            throw new BusinessRuleException("This post has already been published");
        }

        SocialMediaPublisher publisher = publisherFactory.forPlatform(post.getPlatform());
        try {
            publisher.publish(post);
            post.setStatus(SocialMediaPostStatus.POSTED);
            post.setPostedAt(LocalDateTime.now());
            post.setErrorMessage(null);
        } catch (Exception e) {
            post.setStatus(SocialMediaPostStatus.FAILED);
            post.setErrorMessage(e.getMessage());
        }

        post.persistAndFlush();
        return post;
    }

    @Transactional
    public SocialMediaPostEntity retryPost(Long postId) {
        SocialMediaPostEntity post = getById(postId);

        if (post.getStatus() != SocialMediaPostStatus.FAILED) {
            throw new BusinessRuleException("Only failed posts can be retried");
        }

        SocialMediaPublisher publisher = publisherFactory.forPlatform(post.getPlatform());
        try {
            publisher.publish(post);
            post.setStatus(SocialMediaPostStatus.POSTED);
            post.setPostedAt(LocalDateTime.now());
            post.setErrorMessage(null);
        } catch (Exception e) {
            post.setStatus(SocialMediaPostStatus.FAILED);
            post.setErrorMessage(e.getMessage());
        }

        post.persistAndFlush();
        return post;
    }

    @Transactional
    public void cancelPost(Long postId) {
        SocialMediaPostEntity post = getById(postId);

        if (post.getStatus() == SocialMediaPostStatus.POSTED) {
            throw new BusinessRuleException("Cannot cancel a post that has already been published");
        }

        post.delete();
    }

    public SocialMediaPostEntity getById(Long id) {
        return SocialMediaPostEntity.<SocialMediaPostEntity>findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFound("Post not found"));
    }

    public List<SocialMediaPostEntity> listAll() {
        return SocialMediaPostEntity.listAll();
    }

    public List<SocialMediaPostEntity> listByPlatform(SocialMediaPlatform platform) {
        return SocialMediaPostEntity.findByPlatform(platform);
    }

    public List<SocialMediaPostEntity> listByStatus(SocialMediaPostStatus status) {
        return SocialMediaPostEntity.findByStatus(status);
    }

    public List<SocialMediaPostEntity> listByType(SocialMediaPostType postType) {
        return SocialMediaPostEntity.findByPostType(postType);
    }
}