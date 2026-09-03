package com.eduagent.resource.dto;

import lombok.Data;

@Data
public class LearningProfileVO {
    private String course;
    private String topic;
    private String knowledgeBase;
    private String weaknesses;
    private String pace;
    private String resourcePreference;
    private Integer lastScore;
}
