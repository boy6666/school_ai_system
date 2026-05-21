package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.service.AdminStatisticsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {

    private final AdminStatisticsService statisticsService;

    public AdminStatisticsController(AdminStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public Result<Map<String, Object>> overview() {
        return Result.ok(statisticsService.getOverview());
    }

    @GetMapping("/user-growth")
    public Result<Map<String, Object>> userGrowth(@RequestParam(defaultValue = "month") String period) {
        return Result.ok(statisticsService.getUserGrowth(period));
    }

    @GetMapping("/learning")
    public Result<Map<String, Object>> learning(@RequestParam(defaultValue = "week") String period) {
        return Result.ok(statisticsService.getLearningData(period));
    }

    @PostMapping("/export")
    public Result<?> export(@RequestBody Map<String, Object> params) {
        return Result.ok("导出任务已创建");
    }
}
