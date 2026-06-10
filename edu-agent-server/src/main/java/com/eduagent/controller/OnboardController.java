package com.eduagent.controller;

import com.eduagent.agent.AiChatResponse;
import com.eduagent.agent.AiClient;
import com.eduagent.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/onboard")
@RequiredArgsConstructor
public class OnboardController {

    private final AiClient aiClient;

    /**
     * 引导聊天 — 前端发消息 → Java 转发 Python AI → 返回含 profile_complete
     * POST /onboard/chat
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        Long userId;
        try {
            userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            return Result.error("未登录");
        }

        String message = (String) body.getOrDefault("message", "");
        String sessionId = (String) body.getOrDefault("session_id", "onboard_" + System.currentTimeMillis());

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) body.get("profile");

        log.info("===== [引导聊天] userId={}, message={}", userId, message);

        AiChatResponse aiResponse = aiClient.chat(String.valueOf(userId), sessionId, message, profile);

        Map<String, Object> result = new HashMap<>();
        result.put("final_answer", aiResponse.getFinalAnswer());
        result.put("intent", aiResponse.getIntent());
        result.put("profile", aiResponse.getProfile());
        result.put("profile_complete", aiResponse.getProfileComplete());
        result.put("resource_dir", aiResponse.getResourceDir());

        log.info("引导聊天返回: profile_complete={}", aiResponse.getProfileComplete());
        return Result.success(result);
    }
}
