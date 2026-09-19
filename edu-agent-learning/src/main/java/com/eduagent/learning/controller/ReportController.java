package com.eduagent.learning.controller;

import com.eduagent.common.result.Result;
import com.eduagent.learning.common.PageResult;
import com.eduagent.learning.common.RoleGuard;
import com.eduagent.learning.dto.GenerateReportRequest;
import com.eduagent.learning.service.ReportService;
import com.eduagent.learning.vo.ReportVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 学习报告（AI 生成，存 report 表复用）。 */
@Slf4j
@RestController
@RequestMapping("/api/edu-agent-learning/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    public Result<ReportVO> generate(@Valid @RequestBody GenerateReportRequest request) {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(reportService.generateReport(studentId, request));
    }

    @GetMapping
    public Result<PageResult<ReportVO>> list(@RequestParam(name = "page", defaultValue = "1") long page,
                                             @RequestParam(name = "size", defaultValue = "10") long size) {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(reportService.listReports(studentId, page, size));
    }

    @GetMapping("/{id}")
    public Result<ReportVO> detail(@PathVariable("id") Long id) {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(reportService.getReport(studentId, id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long studentId = RoleGuard.currentStudentId();
        reportService.deleteReport(studentId, id);
        return Result.success();
    }
}
