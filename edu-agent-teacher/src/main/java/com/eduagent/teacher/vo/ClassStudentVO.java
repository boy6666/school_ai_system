package com.eduagent.teacher.vo;

import java.time.LocalDateTime;

/**
 * 班级成员出参。studentName 需跨服务拉取（auth），本期可先置 null，由前端显示学号。
 */
public record ClassStudentVO(Long studentId, String studentName, LocalDateTime joinedAt) {
}
