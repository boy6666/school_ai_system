package com.eduagent.teacher.vo;

import java.time.LocalDateTime;

/**
 * 成绩详情出参（含全量 JSON 报告：runResult/staticReport/aiReport + comment）。
 * JSON 字段以 String 承载，前端可直接渲染。submissionId 供教师触发重判分。
 */
public record GradeDetailVO(Long id, Long assignmentId, Long studentId, Long itemId,
                            String type, String language, Long submissionId, String submission,
                            Integer score, Integer status, LocalDateTime gradedAt,
                            String runResult, String staticReport, String aiReport,
                            String comment) {
}
