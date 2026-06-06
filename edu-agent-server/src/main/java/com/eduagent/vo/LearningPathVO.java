package com.eduagent.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class LearningPathVO {
    private String goal;
    private String targetMastery;
    private String estimatedCompletion;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer totalHours;
    private List<StageVO> stages;

    // 新增字段：路径调整建议、应用建议、阶段测评建议
    private String suggestions;
    private String applicationAdvice;
    private String examAdvice;

    // 推荐资源：按类型分组 {doc: [...], video: [...], exercise: [...], code: [...]}
    private Map<String, List<Map<String, String>>> resources;

    // 动态调整记录 [{time: "...", content: "..."}]
    private List<Map<String, String>> adjustRecords;

    // 当前概览
    private Integer masteryRate;      // 当前掌握度 %
    private Integer learningRate;     // 学习中 %
    private Integer unmasteredRate;   // 未掌握 %

    // 推荐学习时段
    private String recommendTime;
}
