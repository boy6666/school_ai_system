package com.eduagent.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 测验作答。resource_id 逻辑引用 resource_db（无外键）。 */
@Data
@TableName("quiz_answer")
public class QuizAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long resourceId;
    private String question;
    private String questionType;
    private String userAnswer;
    private String correctAnswer;
    /** 1 对 0 错 NULL 未判定（AI judge 不可用时的主观题） */
    private Integer isCorrect;
    private String explanation;
    private LocalDateTime createTime;
}
