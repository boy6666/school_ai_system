package com.eduagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AiClient {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private AiConfig aiConfig;

    public AiChatResponse chat(String studentId, String message) {
        String url = aiConfig.getBaseUrl() + "/chat";
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("user_input", message);
        requestBody.put("student_id", studentId);
        requestBody.put("session_id", "api_session");   // 添加 session_id
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        log.info("Calling AI: studentId={}, message={}", studentId, message);
        try {
            AiChatResponse response = restTemplate.postForObject(url, requestEntity, AiChatResponse.class);
            log.info("AI response: intent={}, answerLength={}", response.getIntent(), response.getFinalAnswer().length());
            return response;
        } catch (Exception e) {
            log.error("AI call failed", e);
            // 降级响应
            AiChatResponse fallback = new AiChatResponse();
            fallback.setIntent("fallback");
            fallback.setFinalAnswer("AI服务暂时不可用，请稍后再试。");
            return fallback;
        }
    }
}
