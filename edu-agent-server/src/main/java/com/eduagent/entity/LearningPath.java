package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

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
