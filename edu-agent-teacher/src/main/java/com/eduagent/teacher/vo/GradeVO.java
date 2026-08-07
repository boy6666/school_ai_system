package com.eduagent.teacher.vo;

import java.time.LocalDateTime;

/**
 * 成绩摘要出参（不含全量报告 JSON）。
 */
public record GradeVO(Long id, Long assignmentId, Long studentId, Long itemId,
                      String type, String language, String submission,
                      Integer score, Integer status, LocalDateTime gradedAt,
                      boolean hasAiReport) {
}
