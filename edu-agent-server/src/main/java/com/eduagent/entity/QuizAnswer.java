package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

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
    private Integer isCorrect;
    private String explanation;
    private LocalDateTime createTime;
}
