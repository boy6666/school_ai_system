package com.eduagent.code.vo;

import lombok.Data;

/**
 * 判分结果查询 VO（C1 §2 响应结构，与 teacher 的 grades 列一一对应）。
 * <ul>
 *   <li>stdout / runTimeMs / compileOk → run_result</li>
 *   <li>checkstyle / pmd → static_report（JSON 文本）</li>
 *   <li>aiSuggestion → ai_report</li>
 *   <li>overallScore → score</li>
 * </ul>
 * 内部 scoreDetail 权重明细不对外暴露。
 */
@Data
public class CodeSubmitResultVO {

    private Long submissionId;

    /** 判分状态，见 {@link com.eduagent.code.entity.SubmissionStatus} */
    private Integer status;

    private String stdout;

    private Integer runTimeMs;

    /** 0=未过 1=通过 */
    private Integer compileOk;

    private String checkstyle;

    private String pmd;

    private String aiSuggestion;

    private Integer overallScore;
}
