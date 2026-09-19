package com.eduagent.learning.controller;

import com.eduagent.common.result.Result;
import com.eduagent.learning.common.RoleGuard;
import com.eduagent.learning.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Dashboard：AI 学习总结 / 学习回顾 / 六维评价。 */
@Slf4j
@RestController
@RequestMapping("/api/edu-agent-learning/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @PostMapping("/ai-summary")
    public Result<Map<String, Object>> generateAiSummary() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(dashboardService.generateAiSummary(studentId));
    }

    @GetMapping("/ai-summary")
    public Result<Map<String, Object>> latestAiSummary() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(dashboardService.getLatestAiSummary(studentId));
    }

    @PostMapping("/learning-review")
    public Result<Map<String, Object>> generateLearningReview() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(dashboardService.generateLearningReview(studentId));
    }

    @GetMapping("/learning-review")
    public Result<Map<String, Object>> latestLearningReview() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(dashboardService.getLatestLearningReview(studentId));
    }

    @GetMapping("/evaluation")
    public Result<Map<String, Object>> evaluation() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(dashboardService.evaluation(studentId));
    }
}
