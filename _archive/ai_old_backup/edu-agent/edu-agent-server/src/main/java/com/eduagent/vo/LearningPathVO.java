package com.eduagent.vo;

import lombok.Data;
import java.util.List;

@Data
public class LearningPathVO {
    private String goal;
    private String targetMastery;
    private String estimatedCompletion;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer totalHours;
    private List<StageVO> stages;
}
