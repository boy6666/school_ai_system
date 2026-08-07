package com.eduagent.teacher.vo;

/**
 * 班级概览出参（首页轻量卡片）。
 */
public record ClassOverviewVO(Long classId, String className, int studentCount,
                              double avgMastery, double completionRate,
                              int activeStudents) {
}
