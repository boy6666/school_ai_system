package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String sessionId;
    private String question;
    private String answer;
    private String intent;
    private String intentConfidence;
    private String evaluationReport;
    private String resourceDir;
    private LocalDateTime createTime;
}
