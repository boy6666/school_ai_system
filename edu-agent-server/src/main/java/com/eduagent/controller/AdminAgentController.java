package com.eduagent.controller;

import com.eduagent.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/agents")
public class AdminAgentController {

    private static final List<Map<String, Object>> AGENTS = List.of(
        Map.ofEntries(
                Map.entry("id", 1), Map.entry("name", "ProfileAgent"), Map.entry("type", "profile"),
                Map.entry("description", "学生画像识别"), Map.entry("model", "deepseek-v3"),
                Map.entry("status", "running"), Map.entry("callCount", 1523), Map.entry("activeUsers", 89),
                Map.entry("satisfaction", 92.5), Map.entry("solveRate", 87.3),
                Map.entry("tools", List.of("profile_analysis", "weakness_detection")),
                Map.entry("promptVersion", "v2.1"), Map.entry("updateTime", "2026-05-15 10:30:00")),
        Map.ofEntries(
                Map.entry("id", 2), Map.entry("name", "TutorAgent"), Map.entry("type", "tutor"),
                Map.entry("description", "智能辅导"), Map.entry("model", "deepseek-v3"),
                Map.entry("status", "running"), Map.entry("callCount", 892), Map.entry("activeUsers", 45),
                Map.entry("satisfaction", 88.7), Map.entry("solveRate", 82.1),
                Map.entry("tools", List.of("question_answer", "explain_concept")),
                Map.entry("promptVersion", "v1.8"), Map.entry("updateTime", "2026-05-18 14:20:00")),
        Map.ofEntries(
                Map.entry("id", 3), Map.entry("name", "QuizAgent"), Map.entry("type", "quiz"),
                Map.entry("description", "智能出题"), Map.entry("model", "deepseek-v3"),
                Map.entry("status", "running"), Map.entry("callCount", 2341), Map.entry("activeUsers", 120),
                Map.entry("satisfaction", 90.1), Map.entry("solveRate", 85.6),
                Map.entry("tools", List.of("generate_question", "evaluate_answer")),
                Map.entry("promptVersion", "v3.0"), Map.entry("updateTime", "2026-05-20 09:15:00"))
    );

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        List<Map<String, Object>> filtered = AGENTS.stream()
                .filter(a -> keyword == null || keyword.isEmpty() ||
                        ((String) a.get("name")).toLowerCase().contains(keyword.toLowerCase()))
                .filter(a -> status == null || status.isEmpty() || a.get("status").equals(status))
                .filter(a -> type == null || type.isEmpty() || a.get("type").equals(type))
                .toList();

        return Result.ok(Map.of("list", filtered, "total", filtered.size()));
    }

    @PutMapping("/{id}")
    public Result<?> saveConfig(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return Result.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        return Result.ok(Map.of("success", true));
    }
}
