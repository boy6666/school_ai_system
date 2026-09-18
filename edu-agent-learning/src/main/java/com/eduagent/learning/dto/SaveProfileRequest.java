package com.eduagent.learning.dto;

import lombok.Data;

import java.util.List;

/** 画像保存请求（POST /profile/save）。六维 dimensions 不在此 body，由 AI 合并进 profile_data。 */
@Data
public class SaveProfileRequest {

    private String pace;
    private String learningGoal;
    private String topic;
    private String course;
    private String knowledgeBase;
    private String cognitiveStyle;
    private String overallType;
    private List<String> weaknesses;
    private List<String> resourcePreference;
    private List<String> mistakePatterns;
}
