package com.eduagent.teacher.vo;

import java.time.LocalDateTime;

/**
 * 学生视角的作业卡片：作业信息 + 该生成绩汇总。
 */
public record StudentAssignmentVO(Long assignmentId, Long classId, String title, String type,
                                  LocalDateTime deadline, Integer status,
                                  Integer myScore, Integer totalScore, LocalDateTime submittedAt) {
}
