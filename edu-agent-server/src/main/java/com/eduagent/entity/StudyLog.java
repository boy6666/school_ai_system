package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

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
