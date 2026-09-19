package com.eduagent.learning.service.impl;

import com.eduagent.common.result.ApiException;
import com.eduagent.learning.entity.Report;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.ReportMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplOwnershipTest {

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private AiServiceClient aiServiceClient;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(reportMapper, aiServiceClient,
                new AiResultParser(new ObjectMapper()));
    }

    @Test
    void getReportRejectsOtherStudentsReport() {
        Report report = new Report();
        report.setId(7L);
        report.setStudentId(2002L);
        when(reportMapper.selectById(7L)).thenReturn(report);

        ApiException ex = assertThrows(ApiException.class, () -> service.getReport(1001L, 7L));

        assertEquals(404, ex.getCode());
    }

    @Test
    void deleteReportRejectsOtherStudentsReport() {
        Report report = new Report();
        report.setId(7L);
        report.setStudentId(2002L);
        when(reportMapper.selectById(7L)).thenReturn(report);

        ApiException ex = assertThrows(ApiException.class, () -> service.deleteReport(1001L, 7L));

        assertEquals(404, ex.getCode());
        verify(reportMapper, never()).deleteById(7L);
    }
}
