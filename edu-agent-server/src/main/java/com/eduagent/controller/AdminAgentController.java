package com.eduagent.controller;

import com.eduagent.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/agents")
public class AdminAgentController {

    private static final List<Map<String, Object>> AGENTS = List.of(
        Map.of("id", 1, "name", "ProfileAgent", "type", "profile", "description", "学生画像识别",
                "model", "deepseek-v3", "status", "running", "callCount", 1523, "activeUsers", 89,
                "satisfaction", 92.5, "solveRate", 87.3,
                "tools", List.of("profile_analysis", "weakness_detection"),
                "promptVersion", "v2.1", "updateTime", "2026-05-15 10:30:00"),
        Map.of("id", 2, "name", "TutorAgent", "type", "tutor", "description", "智能辅导",
                "model", "deepseek-v3", "status", "running", "callCount", 892, "activeUsers", 45,
                "satisfaction", 88.7, "solveRate", 82.1,
                "tools", List.of("question_answer", "explain_concept"),
                "promptVersion", "v1.8", "updateTime", "2026-05-18 14:20:00"),
        Map.of("id", 3, "name", "QuizAgent", "type", "quiz", "description", "智能出题",
                "model", "deepseek-v3", "status", "running", "callCount", 2341, "activeUsers", 120,
                "satisfaction", 90.1, "solveRate", 85.6,
                "tools", List.of("generate_question", "evaluate_answer"),
                "promptVersion", "v3.0", "updateTime", "2026-05-20 09:15:00")
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
