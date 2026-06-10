package com.eduagent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.common.Result;
import com.eduagent.dto.GenerateReportRequest;
import com.eduagent.service.ReportService;
import com.eduagent.vo.ReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/reports")
public class ReportController {
    @Autowired
    private ReportService reportService;

    @PostMapping("/generate")
    public Result<ReportVO> generateReport(@Valid @RequestBody GenerateReportRequest request) {
        Long studentId = 1L; // 临时固定，后续从 SecurityContext 获取
        return Result.success(reportService.generateReport(studentId, request));
    }

    @GetMapping("/{id}")
    public Result<ReportVO> getReport(@PathVariable Long id) {
        Long studentId = 1L;
        return Result.success(reportService.getReport(id, studentId));
    }

    @GetMapping
    public Result<Page<ReportVO>> listReports(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size) {
        Long studentId = 1L;
        return Result.success(reportService.listReports(studentId, page, size));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteReport(@PathVariable Long id) {
        Long studentId = 1L;
        reportService.deleteReport(id, studentId);
        return Result.success(null);
    }
}
