package com.eduagent.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class GenerateReportRequest {
    @NotBlank
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String metrics;
}
