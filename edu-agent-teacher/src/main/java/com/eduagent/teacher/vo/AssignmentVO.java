package com.eduagent.teacher.vo;

import java.time.LocalDateTime;

/**
 * 作业列表/摘要出参。
 */
public record AssignmentVO(Long id, Long classId, String title, String type,
                           LocalDateTime deadline, Integer status,
                           LocalDateTime createTime, Integer itemCount, Integer totalScore) {
}
