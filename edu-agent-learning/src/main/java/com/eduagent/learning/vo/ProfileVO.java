package com.eduagent.learning.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 画像出参（camelCase，决议 C4）。六维 dimensions 结构与 edu-agent-ai
 * profile_schema.DimensionState（score/level/evidence）严格对齐。
 */
@Data
public class ProfileVO {

    private Long studentId;
    private Long classId;
    private String major;
    private String grade;
    private String course;
    private String topic;
    private String learningGoal;
    private String knowledgeBase;
    private String cognitiveStyle;
    private String pace;
    private List<String> weaknesses;
    private List<String> mistakePatterns;
    private List<String> resourcePreference;
    private String overallType;
    private Integer lastScore;
    private Boolean profileComplete;
    private Long quizCount;

    /** 六维画像：knowledge_mastery / learning_goal_clarity / cognitive_adaptation / mistake_avoidance / learning_autonomy / overall_level */
    private Map<String, Object> dimensions;

    private List<String> profileSuggestions;
    private String lastSuggestion;
    private LocalDateTime updateTime;
    private Boolean exists;
}
