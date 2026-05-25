package com.eduagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Slf4j
@Component
public class AiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AiClient(@Value("${ai.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    public AiChatResponse chat(String studentId, String sessionId, String message) {
        String url = baseUrl + "/chat";
        AiChatRequest request = new AiChatRequest(message, studentId, sessionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiChatRequest> entity = new HttpEntity<>(request, headers);

        log.info("call AI: url={}", url);

        ResponseEntity<AiChatResponse> response = restTemplate.postForEntity(url, entity, AiChatResponse.class);
        return response.getBody();
    }

    public String healthCheck() {
        try {
            return restTemplate.getForObject(baseUrl + "/health", String.class);
        } catch (Exception e) {
            log.error("AI health check failed", e);
            return "{\"status\":\"error\"}";
        }
    }
}
