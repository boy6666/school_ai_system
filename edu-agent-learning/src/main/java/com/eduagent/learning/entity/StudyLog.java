package com.eduagent.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 学习日志（module: mindmap/quiz/reading/code）。 */
@Data
@TableName("study_logs")
public class StudyLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String module;
    private Integer durationSec;
    private Integer chapterId;
    private Integer noteId;
    private LocalDateTime createdAt;
}
