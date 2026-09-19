package com.eduagent.learning.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiContractSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    void chatUsesSharedJavaMessageContextShape() throws Exception {
        AiChatRequest request = AiChatRequest.of(
                "什么是多态", 1001L, "sess_1", Map.of("course", "JavaSE"));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertEquals(Set.of("message", "context"), fieldNames(json));
        assertEquals("什么是多态", json.path("message").asText());
        assertEquals("1001", json.path("context").path("studentId").asText());
        assertEquals("sess_1", json.path("context").path("sessionId").asText());
        assertEquals("JavaSE", json.path("context").path("profile").path("course").asText());
    }

    @Test
    void resourceUsesSharedJavaModeExtraShape() throws Exception {
        AiResourceRequest request = AiResourceRequest.builder()
                .mode("judge")
                .topic("测验判分")
                .type("judge")
                .extra(Map.of("studentId", "1001", "prompt", "请判分"))
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertEquals(Set.of("mode", "topic", "type", "extra"), fieldNames(json));
        assertEquals("1001", json.path("extra").path("studentId").asText());
        assertEquals("请判分", json.path("extra").path("prompt").asText());
        assertFalse(json.has("resourceType"));
        assertFalse(json.has("studentId"));
    }

    @Test
    void pathKeepsCamelCaseAndCurrentAiCompatibilityAlias() throws Exception {
        AiPathRequest request = AiPathRequest.builder()
                .studentId("1001")
                .prompt("生成路径")
                .profile(Map.of("course", "JavaSE"))
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertEquals(Set.of("studentId", "student_id", "prompt", "profile"), fieldNames(json));
        assertEquals("1001", json.path("studentId").asText());
        assertEquals("1001", json.path("student_id").asText());
    }

    private Set<String> fieldNames(JsonNode node) {
        java.util.Set<String> names = new java.util.HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
