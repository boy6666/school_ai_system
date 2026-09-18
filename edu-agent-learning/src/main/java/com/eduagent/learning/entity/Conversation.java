package com.eduagent.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 对话历史（智能辅导 / 引导对话留存）。 */
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
