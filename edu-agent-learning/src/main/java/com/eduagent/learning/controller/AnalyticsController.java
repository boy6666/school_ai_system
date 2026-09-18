package com.eduagent.learning.controller;

import com.eduagent.common.result.Result;
import com.eduagent.learning.common.RoleGuard;
import com.eduagent.learning.service.AnalyticsService;
import com.eduagent.learning.vo.StudentAnalyticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 学情统计聚合（供 edu-agent-teacher 看板调用，T/A 角色）。 */
@Slf4j
@RestController
@RequestMapping("/api/edu-agent-learning/analytics/student")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{studentId}")
    public Result<StudentAnalyticsVO> student(@PathVariable Long studentId) {
        RoleGuard.requireTeacherOrAdmin();
        return Result.success(analyticsService.studentAnalytics(studentId));
    }

    @GetMapping("/{studentId}/progress")
    public Result<Map<String, Object>> progress(@PathVariable Long studentId) {
        RoleGuard.requireTeacherOrAdmin();
        return Result.success(analyticsService.studentProgress(studentId));
    }
}
