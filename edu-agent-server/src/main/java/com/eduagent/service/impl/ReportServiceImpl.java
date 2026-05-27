package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.agent.AiClient;
import com.eduagent.agent.AiChatResponse;
import com.eduagent.dto.GenerateReportRequest;
import com.eduagent.entity.Report;
import com.eduagent.mapper.ReportMapper;
import com.eduagent.service.ReportService;
import com.eduagent.vo.ReportVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private AiClient aiClient;

    @Override
    public ReportVO generateReport(Long studentId, GenerateReportRequest req) {
        String prompt = String.format("请为我的学习生成一份报告，标题：%s，时间范围：%s 至 %s",
                req.getTitle(), req.getPeriodStart(), req.getPeriodEnd());
        AiChatResponse aiResp = aiClient.chat(studentId.toString(), prompt);
        String content = aiResp.getFinalAnswer();

        Report report = new Report();
        report.setStudentId(studentId);
        report.setTitle(req.getTitle());
        report.setPeriodStart(req.getPeriodStart());
        report.setPeriodEnd(req.getPeriodEnd());
        report.setMetrics(req.getMetrics());
        report.setContent(content);
        reportMapper.insert(report);
        return toVO(report);
    }

    @Override
    public ReportVO getReport(Long reportId, Long studentId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null || !report.getStudentId().equals(studentId))
            throw new RuntimeException("报告不存在或无权限");
        return toVO(report);
    }

    @Override
    public Page<ReportVO> listReports(Long studentId, Integer page, Integer size) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(Report::getStudentId, studentId)
                .orderByDesc(Report::getCreateTime);
        Page<Report> pageResult = reportMapper.selectPage(new Page<>(page, size), wrapper);
        Page<ReportVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public void deleteReport(Long reportId, Long studentId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null || !report.getStudentId().equals(studentId))
            throw new RuntimeException("报告不存在或无权限");
        reportMapper.deleteById(reportId);
    }

    private ReportVO toVO(Report report) {
        ReportVO vo = new ReportVO();
        BeanUtils.copyProperties(report, vo);
        return vo;
    }
}
