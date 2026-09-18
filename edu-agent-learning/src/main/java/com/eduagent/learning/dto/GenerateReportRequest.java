package com.eduagent.learning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/** 报告生成请求（POST /reports/generate）。 */
@Data
public class GenerateReportRequest {

    @NotBlank(message = "报告标题不能为空")
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String metrics;
}
