package com.codelevel.module.integration.infra.publisher;

import com.codelevel.module.integration.infra.publisher.instagram.InstagramPublisher;
import com.codelevel.module.integration.infra.publisher.linkedin.LinkedInPublisher;
import com.codelevel.module.integration.persistence.entity.enums.SocialMediaPlatform;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SocialMediaPublisherFactory {

    private final MockSocialMediaPublisher mockPublisher;
    private final LinkedInPublisher linkedInPublisher;
    private final InstagramPublisher instagramPublisher;
    private final boolean mockPublishing;

    @Inject
    public SocialMediaPublisherFactory(
            MockSocialMediaPublisher mockPublisher,
            LinkedInPublisher linkedInPublisher,
            InstagramPublisher instagramPublisher,
            @ConfigProperty(name = "integration.mock-publishing", defaultValue = "true") boolean mockPublishing) {
        this.mockPublisher = mockPublisher;
        this.linkedInPublisher = linkedInPublisher;
        this.instagramPublisher = instagramPublisher;
        this.mockPublishing = mockPublishing;
    }

    public SocialMediaPublisher forPlatform(SocialMediaPlatform platform) {
        if (mockPublishing) {
            return mockPublisher;
        }
        return switch (platform) {
            case LINKEDIN -> linkedInPublisher;
            case INSTAGRAM -> instagramPublisher;
        };
    }
}