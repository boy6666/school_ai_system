package com.eduagent.learning.service;

import com.eduagent.learning.common.PageResult;
import com.eduagent.learning.dto.GenerateReportRequest;
import com.eduagent.learning.vo.ReportVO;

public interface ReportService {

    ReportVO generateReport(Long studentId, GenerateReportRequest request);

    PageResult<ReportVO> listReports(Long studentId, long page, long size);

    ReportVO getReport(Long studentId, Long reportId);

    void deleteReport(Long studentId, Long reportId);
}
