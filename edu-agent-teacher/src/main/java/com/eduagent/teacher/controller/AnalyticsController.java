package com.eduagent.teacher.controller;

import com.eduagent.common.result.Result;
import com.eduagent.teacher.service.AnalyticsService;
import com.eduagent.teacher.vo.ClassAnalyticsVO;
import com.eduagent.teacher.vo.ClassOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学情看板（T）。ECharts 直接消费聚合结果；消费 study.progress 近实时。
 */
@RestController
@RequestMapping("/api/edu-agent-teacher/classes")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/{id}/analytics")
    public Result<ClassAnalyticsVO> analytics(@PathVariable("id") Long classId) {
        return Result.success(analyticsService.classAnalytics(classId));
    }

    @GetMapping("/{id}/overview")
    public Result<ClassOverviewVO> overview(@PathVariable("id") Long classId) {
        return Result.success(analyticsService.classOverview(classId));
    }
}
