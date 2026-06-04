package com.codelevel.module.integration.infra.publisher.linkedin.dto;

import java.util.Map;

public class LinkedInUgcPostRequest {

    public String author;
    public String lifecycleState;
    public Map<String, Object> specificContent;
    public Map<String, Object> visibility;

    public static LinkedInUgcPostRequest textPost(String authorUrn, String text) {
        var request = new LinkedInUgcPostRequest();
        request.author = authorUrn;
        request.lifecycleState = "PUBLISHED";
        request.specificContent = Map.of(
                "com.linkedin.ugc.ShareContent", Map.of(
                        "shareCommentary", Map.of("text", text),
                        "shareMediaCategory", "NONE"
                )
        );
        request.visibility = Map.of(
                "com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC"
        );
        return request;
    }
}