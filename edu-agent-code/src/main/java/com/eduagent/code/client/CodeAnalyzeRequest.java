package com.eduagent.code.client;

import lombok.Data;

/**
 * 调 ai-service {@code POST /api/ai/code/analyze} 的请求体（dev-wuyoucheng §1.3.4）。
 * context 里把判分阶段事实传给 AI，AI 只做参考反馈、不改判。
 */
@Data
public class CodeAnalyzeRequest {

    private String language;
    private String sourceCode;
    private Ctx context;

    @Data
    public static class Ctx {
        private Long assignmentItemId;
        private String studentId;
        private Integer compileOk;
        private Integer checkstyleErrors;
        private Integer pmdViolations;
        private Integer runPassed;
        private String runStdout;
        private String expectedOutput;
    }
}
