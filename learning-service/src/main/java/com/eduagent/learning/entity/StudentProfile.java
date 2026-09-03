package com.eduagent.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_profiles")
public class StudentProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String course;
    private String topic;
    private String knowledgeBase;
    private String weaknesses;
    private String pace;
    private String resourcePreference;
    private Integer lastScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
