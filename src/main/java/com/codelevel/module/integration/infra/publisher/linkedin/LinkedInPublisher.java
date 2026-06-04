package com.codelevel.module.integration.infra.publisher.linkedin;

import com.codelevel.module.integration.infra.publisher.SocialMediaPublisher;
import com.codelevel.module.integration.infra.publisher.linkedin.client.LinkedInApiClient;
import com.codelevel.module.integration.infra.publisher.linkedin.dto.LinkedInUgcPostRequest;
import com.codelevel.module.integration.persistence.entity.SocialMediaPostEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class LinkedInPublisher implements SocialMediaPublisher {

    private static final Logger log = Logger.getLogger(LinkedInPublisher.class);

    private final LinkedInApiClient client;
    private final Optional<String> accessToken;
    private final Optional<String> authorUrn;

    @Inject
    public LinkedInPublisher(
            @RestClient LinkedInApiClient client,
            @ConfigProperty(name = "integration.linkedin.access-token") Optional<String> accessToken,
            @ConfigProperty(name = "integration.linkedin.author-urn") Optional<String> authorUrn) {
        this.client = client;
        this.accessToken = accessToken;
        this.authorUrn = authorUrn;
    }

    @Override
    public void publish(SocialMediaPostEntity post) {
        if (accessToken.isEmpty() || accessToken.get().isBlank()
                || authorUrn.isEmpty() || authorUrn.get().isBlank()) {
            throw new IllegalStateException("LinkedIn credentials not configured (LINKEDIN_ACCESS_TOKEN, LINKEDIN_AUTHOR_URN)");
        }

        LinkedInUgcPostRequest request = LinkedInUgcPostRequest.textPost(authorUrn.get(), post.getContent());

        log.infof("Publishing to LinkedIn: postId=%d type=%s", post.getId(), post.getPostType());
        client.createPost("Bearer " + accessToken.get(), "2.0.0", request);
        log.infof("LinkedIn publishing completed: postId=%d", post.getId());
    }
}