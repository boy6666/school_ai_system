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
        Map.of("id", 1, "title", "AI生成的习题集", "type", "resource", "source", "auto",
                "riskLevel", "low", "status", "pending", "submitter", "系统", "submitTime",
                "2026-05-18 08:00:00", "content", "包含50道选择题"),
        Map.of("id", 2, "title", "用户上传的学习笔记", "type", "resource", "source", "user",
                "riskLevel", "middle", "status", "pending", "submitter", "student001", "submitTime",
                "2026-05-19 14:00:00", "content", "包含外链")
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
