package com.eduagent.code.service;

import com.eduagent.code.entity.CodeCheckReport;
import com.eduagent.code.mapper.CodeCheckReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 判分报告落库（code_check_reports，一场判分一行）。
 */
@Service
@RequiredArgsConstructor
public class CodeCheckReportService {

    private final CodeCheckReportMapper reportMapper;

    public void save(CodeCheckReport report) {
        reportMapper.insert(report);
    }
}
