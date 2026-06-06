package com.eduagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        return chat(studentId, sessionId, message, null);
    }

    public AiChatResponse chat(String studentId, String sessionId, String message, java.util.Map<String, Object> profile) {
        String url = baseUrl + "/chat";
        AiChatRequest request = new AiChatRequest(message, studentId, sessionId, profile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiChatRequest> entity = new HttpEntity<>(request, headers);

        log.info("call AI: url={}", url);

        ResponseEntity<AiChatResponse> response = restTemplate.postForEntity(url, entity, AiChatResponse.class);
        return response.getBody();
    }

    /**
     * 通用 POST 调 AI 任意端点
     */
    public String post(String endpoint, Object body) {
        String url = baseUrl + endpoint;
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(body);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            log.info("call AI: url={}", url);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("AI call failed: url={}", url, e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
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
