package com.eduagent.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 学习任务（status: todo/doing/done；stage: today/week/exam/practice）。 */
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
