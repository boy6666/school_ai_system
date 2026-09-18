package com.eduagent.learning.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 学习报告出参。 */
@Data
public class ReportVO {

    private Long id;
    private Long studentId;
    private String title;
    private String content;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String metrics;
    private LocalDateTime createTime;
}
