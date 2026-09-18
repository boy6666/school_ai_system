package com.eduagent.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 学习路径。steps 为 LearningPathVO 序列化 JSON。 */
@Data
@TableName("learning_paths")
public class LearningPath {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String steps;
    private Integer progress;
    private String pace;
    private String goal;
    private String suggestions;
    private String recommendations;
    private String examAdvice;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
