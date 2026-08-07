package com.eduagent.teacher.vo;

import java.util.List;
import java.util.Map;

/**
 * 班级学情看板出参（ECharts 直接消费）。
 */
public record ClassAnalyticsVO(Long classId, String className, int studentCount,
                               double avgMastery, double avgPathProgress, double avgStudySec,
                               List<MasteryDist> masteryDist, Map<String, Double> dimensionAvg,
                               List<TaskEntry> taskCompletion, List<WeakTopic> weakTopics,
                               List<TrendDay> trend) {

    public record MasteryDist(String level, int count) {
    }

    public record TaskEntry(Long studentId, String name, int progress, Integer lastScore) {
    }

    public record WeakTopic(String topic, int count) {
    }

    public record TrendDay(String day, int activeStudents) {
    }
}
