package com.codelevel.module.integration.infra.publisher;

import com.codelevel.module.integration.persistence.entity.SocialMediaPostEntity;

public interface SocialMediaPublisher {

    void publish(SocialMediaPostEntity post);
}