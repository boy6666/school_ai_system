package com.eduagent.learning.dto;

import lombok.Data;

/** 测验判分请求（POST /quiz/judge）。选择题本地比对，主观/代码题走 AI judge。 */
@Data
public class JudgeRequest {

    /** 逻辑引用 resource_db，可空 */
    private Long resourceId;
    private String question;
    /** choice / short / code 等 */
    private String questionType;
    private String userAnswer;
    private String correctAnswer;
    private String explanation;
}
