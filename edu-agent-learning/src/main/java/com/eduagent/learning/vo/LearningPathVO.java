package com.eduagent.learning.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 学习路径出参。字段沿用单体 vo/LearningPathVO.java。
 * 分工（决议 C9）：goal/targetMastery/totalHours/masteryRate/stages/suggestions/
 * applicationAdvice/examAdvice/recommendTime 来自 AI；totalTasks/completedTasks/
 * learningRate/unmasteredRate 与 tasks[].id 由本服务自算补全。
 */
@Data
public class LearningPathVO {

    private String goal;
    private String targetMastery;
    private String estimatedCompletion;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer totalHours;
    private List<StageVO> stages;
    private String suggestions;
    private String applicationAdvice;
    private String examAdvice;
    private Map<String, List<Map<String, String>>> resources;
    private List<Map<String, String>> adjustRecords;
    private Integer masteryRate;
    private Integer learningRate;
    private Integer unmasteredRate;
    private String recommendTime;
}
