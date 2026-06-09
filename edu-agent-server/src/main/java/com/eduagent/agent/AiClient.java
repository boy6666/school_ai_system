package com.eduagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class AiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AiClient(@Value("${ai.base-url}") String baseUrl, @Value("${ai.timeout:60000}") int timeout) {
        this.baseUrl = baseUrl;
        org.springframework.boot.web.client.RestTemplateBuilder builder = new org.springframework.boot.web.client.RestTemplateBuilder();
        this.restTemplate = builder
            .setConnectTimeout(java.time.Duration.ofMillis(30000))
            .setReadTimeout(java.time.Duration.ofMillis(timeout))
            .build();
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

    /**
     * 调用 AI 生成学习资源
     */
    public Map<String, Object> generateResource(String studentId, String chapter, String topic,
                                                  String resourceType, String difficulty,
                                                  Map<String, Object> profile) {
        String url = baseUrl + "/resource/generate";
        Map<String, Object> body = new HashMap<>();
        body.put("student_id", studentId);
        body.put("chapter", chapter);
        body.put("topic", topic);
        body.put("resourceType", resourceType);
        body.put("difficulty", difficulty);
        body.put("profile", profile);

        // 构建 prompt
        String prompt = buildResourcePrompt(chapter, topic, resourceType, difficulty, profile);
        body.put("prompt", prompt);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(body);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            log.info("call AI generateResource: url={}, type={}, difficulty={}", url, resourceType, difficulty);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String resultStr = response.getBody();

            Map<String, Object> result = mapper.readValue(resultStr, Map.class);
            // 统一返回格式：{ content: "..." }
            if (result.containsKey("content")) {
                return result;
            }
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("content", resultStr);
            return fallback;
        } catch (Exception e) {
            log.error("AI generateResource failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("content", "AI 生成失败，请稍后重试");
            error.put("error", e.getMessage());
            return error;
        }
    }

    private String buildResourcePrompt(String chapter, String topic, String resourceType,
                                        String difficulty, Map<String, Object> profile) {
        StringBuilder sb = new StringBuilder();
        // 画像信息
        if (profile != null && !profile.isEmpty()) {
            sb.append("学生画像信息：\n");
            if (profile.get("course") != null) sb.append("- 课程：").append(profile.get("course")).append("\n");
            if (profile.get("topic") != null) sb.append("- 当前主题：").append(profile.get("topic")).append("\n");
            if (profile.get("pace") != null) sb.append("- 学习节奏：").append(profile.get("pace")).append("\n");
            if (profile.get("weaknesses") != null) sb.append("- 薄弱点：").append(profile.get("weaknesses")).append("\n");
            if (profile.get("knowledge_base") != null) sb.append("- 知识基础：").append(profile.get("knowledge_base")).append("\n");
            if (profile.get("resource_preference") != null) sb.append("- 资源偏好：").append(profile.get("resource_preference")).append("\n");
            sb.append("\n");
        }

        sb.append("请为「").append(chapter).append("」的「").append(topic).append("」知识点");

        switch (resourceType) {
            case "mindmap":
                sb.append("生成思维导图，只返回 JSON 格式的树形结构数据，不要任何 Markdown 标记、代码块或额外文字。格式必须为：\n{\"id\":\"root\",\"topic\":\"中心主题\",\"children\":[{\"id\":\"n1\",\"topic\":\"一级分支\",\"children\":[{\"id\":\"n2\",\"topic\":\"二级分支\"}]}]}\n每个节点必须有 id（唯一字符串）和 topic（显示文本），children 可选。层级嵌套表示树形结构。");
                break;
            case "quiz":
                sb.append("生成5道练习题（选择题或简答题）。返回 JSON 数组格式，每个元素包含 question、options（可选）、answer、explanation 字段。");
                break;
            case "reading":
                sb.append("生成约500字的拓展阅读材料，包括进阶概念、应用场景和推荐学习方向。使用 Markdown 格式。");
                break;
            case "code":
                sb.append("生成一个可运行的 Java 代码案例，包含详细注释。使用 Markdown 代码块标注。");
                break;
            case "learning_path":
                sb.append("生成个性化的学习路径规划。必须返回 JSON 格式，包含 goal（学习目标）、stages（阶段数组），每个 stage 包含 name（阶段名）和 tasks（任务数组），每个 task 包含 title、duration（分钟）、status=0、progress=0。直接返回纯 JSON，不要 Markdown 标记。");
                break;
        }

        sb.append("难度：").append(difficulty).append("。");
        if ("easy".equals(difficulty)) {
            sb.append("使用简单语言，减少专业术语，适合零基础学习者。");
        } else if ("hard".equals(difficulty)) {
            sb.append("深入技术细节和底层原理，适合有一定基础的学习者。");
        } else {
            sb.append("使用标准的教学语言，兼顾易理解性和知识深度。");
        }

        sb.append("只返回内容，不要额外说明。");
        return sb.toString();
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
