package com.eduagent.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.learning.common.PageResult;
import com.eduagent.learning.dto.GenerateReportRequest;
import com.eduagent.learning.dto.ai.AiChatRequest;
import com.eduagent.learning.entity.Report;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.ReportMapper;
import com.eduagent.learning.service.ReportService;
import com.eduagent.learning.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学习报告。AI 生成走 edu-agent-ai /chat（不依赖 resource 域）；AI 不可用时落模板文案，不阻塞生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final AiServiceClient aiServiceClient;
    private final AiResultParser aiResultParser;

    @Override
    public ReportVO generateReport(Long studentId, GenerateReportRequest req) {
        String prompt = String.format(
                "请为我的学习生成一份报告，标题：%s，时间范围：%s 至 %s。%s\n"
                        + "内容包含：学习内容回顾、掌握情况评估、薄弱点分析、下一步建议。500 字以内，Markdown 格式。",
                req.getTitle(), req.getPeriodStart(), req.getPeriodEnd(),
                req.getMetrics() != null ? "参考指标：" + req.getMetrics() + "。" : "");

        String content;
        try {
            AiChatRequest aiReq = AiChatRequest.of(prompt, studentId,
                    "report_" + System.currentTimeMillis(), null);
            var result = aiResultParser.parseChatResult(aiServiceClient.chat(aiReq));
            content = result != null ? result.getFinalAnswer() : null;
        } catch (Exception e) {
            log.warn("[Report] AI 生成失败, 走降级模板: {}", e.getMessage());
            content = null;
        }
        if (content == null || content.isBlank()) {
            content = "报告《" + req.getTitle() + "》生成中遇到 AI 服务波动，请稍后重新生成。"
                    + "（时间段：" + req.getPeriodStart() + " ~ " + req.getPeriodEnd() + "）";
        }

        Report report = new Report();
        report.setStudentId(studentId);
        report.setTitle(req.getTitle());
        report.setPeriodStart(req.getPeriodStart());
        report.setPeriodEnd(req.getPeriodEnd());
        report.setMetrics(req.getMetrics());
        report.setContent(content);
        report.setCreateTime(LocalDateTime.now());
        reportMapper.insert(report);
        return toVO(report);
    }

    @Override
    public PageResult<ReportVO> listReports(Long studentId, long page, long size) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(Report::getStudentId, studentId)
                .orderByDesc(Report::getCreateTime);
        Page<Report> result = reportMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.from(result, this::toVO);
    }

    @Override
    public ReportVO getReport(Long studentId, Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null || !report.getStudentId().equals(studentId)) {
            throw new ApiException(ErrorCode.NOT_FOUND.getCode(), "报告不存在或无权限");
        }
        return toVO(report);
    }

    @Override
    public void deleteReport(Long studentId, Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null || !report.getStudentId().equals(studentId)) {
            throw new ApiException(ErrorCode.NOT_FOUND.getCode(), "报告不存在或无权限");
        }
        reportMapper.deleteById(reportId);
    }

    private ReportVO toVO(Report report) {
        ReportVO vo = new ReportVO();
        BeanUtils.copyProperties(report, vo);
        return vo;
    }
}
