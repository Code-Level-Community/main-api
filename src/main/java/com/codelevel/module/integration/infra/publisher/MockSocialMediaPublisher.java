package com.codelevel.module.integration.infra.publisher;

import com.codelevel.module.integration.persistence.entity.SocialMediaPostEntity;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MockSocialMediaPublisher implements SocialMediaPublisher {

    private static final Logger log = Logger.getLogger(MockSocialMediaPublisher.class);

    @Override
    public void publish(SocialMediaPostEntity post) {
        log.infof("Mock publish: post=%d platform=%s type=%s", post.getId(), post.getPlatform(), post.getPostType());
    }
}