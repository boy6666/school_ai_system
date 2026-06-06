package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_profiles")
public class StudentProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String course;
    private String topic;
    private String learningGoal;
    private String knowledgeBase;
    private String weaknesses;
    private String mistakePatterns;
    private String resourcePreference;
    private String cognitiveStyle;
    private String pace;
    private Integer lastScore;
    private String profileData;
    private String profileSuggestions;
    private String lastSuggestion;
    private String overallType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
