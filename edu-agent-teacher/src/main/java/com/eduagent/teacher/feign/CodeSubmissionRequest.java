package com.eduagent.teacher.feign;

import lombok.Builder;
import lombok.Data;

/**
 * 代码判分请求体（对齐《契约对齐决议》C1 / §B.4.2）。
 */
@Data
@Builder
public class CodeSubmissionRequest {

    private Long studentId;
    private Long assignmentId;
    private Long assignmentItemId;
    private String language;
    private String sourceCode;
    /** 期望输出（来自题目 answer），参与判分权重 */
    private String expectedOutput;
    private String className;
}
