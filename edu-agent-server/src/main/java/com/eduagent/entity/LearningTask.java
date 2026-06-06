package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("learning_tasks")
public class LearningTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String courseName;
    private String chapterName;
    private String stage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String priority;
    private String status;
    private Integer progress;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
