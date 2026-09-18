package com.eduagent.learning.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 教师看板单学生学情聚合（供 edu-agent-teacher Feign 消费）。 */
@Data
public class StudentAnalyticsVO {

    private Long studentId;
    private Long classId;
    private String course;
    private String topic;
    private Integer lastScore;
    private Boolean profileComplete;
    private Map<String, Object> dimensions;

    private PathSummary path;
    private StudySummary study;
    private QuizSummary quiz;
    private List<RecentReport> recentReports;

    @Data
    public static class PathSummary {
        private String goal;
        private Integer progress;
        private Integer completedTasks;
        private Integer totalTasks;
    }

    @Data
    public static class StudySummary {
        private Integer totalSec;
        private List<Map<String, Object>> modules;
    }

    @Data
    public static class QuizSummary {
        private Long answered;
        private Long wrong;
        private Double accuracy;
    }

    @Data
    public static class RecentReport {
        private Long id;
        private String title;
        private LocalDateTime createTime;
    }
}
