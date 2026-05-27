package com.eduagent.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {
    private List<TodayTask> todayTasks;
    private List<ReviewSubject> reviewSubjects;
    private List<ProgressSubject> learningProgress;
    private Goal goal;
    private Summary summary;
    private Evaluation evaluation;
    private Rhythm rhythm;
    private Plan plan;

    @Data
    public static class TodayTask {
        private String name;
        private Integer duration;
    }
    @Data
    public static class ReviewSubject {
        private String name;
        private Double hours;
    }
    @Data
    public static class ProgressSubject {
        private String name;
        private Integer percent;
        private Double hours;
    }
    @Data
    public static class Goal {
        private Double totalHours;
        private Integer completedTopics;
    }
    @Data
    public static class Summary {
        private Integer knowledgeGraph;
        private Integer notes;
        private Integer reflection;
        private Integer harvest;
        private Integer insight;
    }
    @Data
    public static class Evaluation {
        private String goal;
        private String cognitiveStyle;
        private String weakPoints;
        private String interestPreference;
    }
    @Data
    public static class Rhythm {
        private String dailyAmount;
        private String dailyDuration;
    }
    @Data
    public static class Plan {
        private String dailyPlan;
        private String dailyTime;
    }
}
