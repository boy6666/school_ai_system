package com.eduagent.learning.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/** edu-agent-ai /chat 的统一 Java Feign 请求：{message, context}。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    private String message;

    /** studentId、sessionId、profile 以及其他业务上下文。 */
    private Map<String, Object> context;

    public static AiChatRequest of(String message, Long studentId, String sessionId,
                                   Map<String, Object> profile) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("studentId", String.valueOf(studentId));
        context.put("sessionId", sessionId);
        if (profile != null && !profile.isEmpty()) {
            context.put("profile", profile);
        }
        return new AiChatRequest(message, context);
    }
}
