package com.codelevel.module.integration.infra.publisher.instagram;

import com.codelevel.module.integration.infra.publisher.SocialMediaPublisher;
import com.codelevel.module.integration.infra.publisher.instagram.client.InstagramApiClient;
import com.codelevel.module.integration.infra.publisher.instagram.dto.InstagramContainerResponse;
import com.codelevel.module.integration.persistence.entity.SocialMediaPostEntity;
import com.codelevel.shared.exception.BusinessRuleException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class InstagramPublisher implements SocialMediaPublisher {

    private static final Logger log = Logger.getLogger(InstagramPublisher.class);

    private final InstagramApiClient client;
    private final Optional<String> accessToken;
    private final Optional<String> igUserId;

    @Inject
    public InstagramPublisher(
            @RestClient InstagramApiClient client,
            @ConfigProperty(name = "integration.instagram.access-token") Optional<String> accessToken,
            @ConfigProperty(name = "integration.instagram.user-id") Optional<String> igUserId) {
        this.client = client;
        this.accessToken = accessToken;
        this.igUserId = igUserId;
    }

    @Override
    public void publish(SocialMediaPostEntity post) {
        if (accessToken.isEmpty() || accessToken.get().isBlank()
                || igUserId.isEmpty() || igUserId.get().isBlank()) {
            throw new IllegalStateException("Instagram credentials not configured (INSTAGRAM_ACCESS_TOKEN, INSTAGRAM_USER_ID)");
        }
        if (post.getMediaUrl() == null || post.getMediaUrl().isBlank()) {
            throw new BusinessRuleException("Instagram posts require an image (mediaUrl is required)");
        }

        log.infof("Creating Instagram container: postId=%d", post.getId());
        InstagramContainerResponse container = client.createContainer(
                igUserId.get(),
                post.getMediaUrl(),
                post.getContent(),
                accessToken.get()
        );

        log.infof("Publishing Instagram container: postId=%d containerId=%s", post.getId(), container.id());
        client.publishContainer(igUserId.get(), container.id(), accessToken.get());
        log.infof("Instagram publishing completed: postId=%d", post.getId());
    }
}