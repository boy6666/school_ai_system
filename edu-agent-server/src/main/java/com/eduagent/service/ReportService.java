package com.eduagent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.dto.GenerateReportRequest;
import com.eduagent.vo.ReportVO;

public interface ReportService {
    ReportVO generateReport(Long studentId, GenerateReportRequest req);
    ReportVO getReport(Long reportId, Long studentId);
    Page<ReportVO> listReports(Long studentId, Integer page, Integer size);
    void deleteReport(Long reportId, Long studentId);
}
