package com.eduagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

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
     * 调用 AI 智能体生成章节资源（思维导图/练习题/拓展阅读/代码案例）
     */
    public Map<String, Object> generateResource(
            String studentId,
            String chapter,
            String topic,
            String resourceType,
            String difficulty,
            Map<String, Object> profile) {

        String url = baseUrl + "/resource/generate";

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("student_id", studentId != null ? studentId : "student_001");
        requestBody.put("chapter", chapter != null ? chapter : "");
        requestBody.put("topic", topic != null ? topic : chapter);
        requestBody.put("resourceType", resourceType != null ? resourceType : "mindmap");
        requestBody.put("difficulty", difficulty != null ? difficulty : "medium");
        requestBody.put("chapter_id", "");
        requestBody.put("course_id", "");
        if (profile != null) {
            requestBody.put("profile", profile);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.info("call AI resource generate: url={}, type={}, chapter={}, difficulty={}", url, resourceType, chapter, difficulty);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                Map<String, Object> fallback = new java.util.HashMap<>();
                fallback.put("content", "AI 服务返回为空，请稍后重试");
                fallback.put("resourceType", resourceType);
                fallback.put("chapter", chapter);
                return fallback;
            }
            return body;
        } catch (Exception e) {
            log.error("AI resource generate failed", e);
            Map<String, Object> fallback = new java.util.HashMap<>();
            fallback.put("content", "AI 生成失败：" + e.getMessage());
            fallback.put("resourceType", resourceType);
            fallback.put("chapter", chapter);
            return fallback;
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
