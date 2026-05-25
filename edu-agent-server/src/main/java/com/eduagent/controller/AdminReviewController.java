package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.dto.ReviewActionRequest;
import com.eduagent.security.AdminOperation;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private static final List<Map<String, Object>> REVIEWS = List.of(
        Map.ofEntries(
                Map.entry("id", 1), Map.entry("title", "AI生成的习题集"), Map.entry("type", "resource"),
                Map.entry("source", "auto"), Map.entry("riskLevel", "low"), Map.entry("status", "pending"),
                Map.entry("submitter", "系统"), Map.entry("submitTime", "2026-05-18 08:00:00"),
                Map.entry("content", "包含50道选择题")),
        Map.ofEntries(
                Map.entry("id", 2), Map.entry("title", "用户上传的学习笔记"), Map.entry("type", "resource"),
                Map.entry("source", "user"), Map.entry("riskLevel", "middle"), Map.entry("status", "pending"),
                Map.entry("submitter", "student001"), Map.entry("submitTime", "2026-05-19 14:00:00"),
                Map.entry("content", "包含外链"))
    );

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String riskLevel) {

        List<Map<String, Object>> filtered = REVIEWS.stream()
                .filter(r -> keyword == null || keyword.isEmpty() ||
                        ((String) r.get("title")).contains(keyword))
                .filter(r -> status == null || status.isEmpty() || r.get("status").equals(status))
                .filter(r -> type == null || type.isEmpty() || r.get("type").equals(type))
                .filter(r -> riskLevel == null || riskLevel.isEmpty() || r.get("riskLevel").equals(riskLevel))
                .toList();

        return Result.ok(Map.of("list", filtered, "total", filtered.size()));
    }

    @PostMapping("/{id}/approve")
    @AdminOperation(value = "审核通过", targetType = "review")
    public Result<?> approve(@PathVariable Long id) {
        return Result.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/reject")
    @AdminOperation(value = "审核拒绝", targetType = "review")
    public Result<?> reject(@PathVariable Long id, @RequestBody ReviewActionRequest request) {
        return Result.ok(Map.of("success", true));
    }
}
