package com.eduagent.learning.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 智能辅导回复。字段沿用单体 vo/TutorReplyVO.java。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorReplyVO {

    private String question;
    private String answer;
    private String intent;
    private String routeReason;
    /** 形如 "掌握度: 72"（取 evaluationReport.understanding_score） */
    private String evaluation;
    private String resourceDir;
}
