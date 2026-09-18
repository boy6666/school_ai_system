package com.eduagent.learning.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生画像。weaknesses/mistakePatterns/resourcePreference/profileData 为 JSON 字符串列。
 */
@Data
@TableName("student_profiles")
public class StudentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑关联 auth_db.users.id（无外键） */
    private Long studentId;

    /** 逻辑归属班级 teacher_db.classes.id（无外键） */
    private Long classId;

    private String major;
    private String grade;
    private String course;
    private String topic;
    private String learningGoal;
    private String knowledgeBase;
    private String cognitiveStyle;
    private String pace;
    private String weaknesses;
    private String mistakePatterns;
    private String resourcePreference;
    private String overallType;
    private Integer lastScore;
    /** 六维画像 JSON，结构对齐 edu-agent-ai profile_schema 的 DimensionState */
    private String profileData;
    private String profileSuggestions;
    private String lastSuggestion;
    /** 0 未完成引导 1 已完成 */
    private Integer profileComplete;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
