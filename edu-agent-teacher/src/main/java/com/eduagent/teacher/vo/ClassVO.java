package com.eduagent.teacher.vo;

import java.time.LocalDateTime;

/**
 * 班级出参 VO。studentCount 由详情查询时填充（列表可为 null）。
 */
public record ClassVO(Long id, String name, Long teacherId, String course,
                      String semester, Integer status, LocalDateTime createTime,
                      Integer studentCount) {
}
