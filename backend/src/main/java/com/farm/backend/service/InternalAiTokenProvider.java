package com.farm.backend.service;

import org.springframework.stereotype.Component;

@Component
public class InternalAiTokenProvider {

    private static final String DEFAULT_TOKEN = "INTERNAL_AI_TOKEN";
    private final String token = System.getenv().getOrDefault("AI_INTERNAL_TOKEN", DEFAULT_TOKEN);

    public String getToken() {
        return token;
    }
}
