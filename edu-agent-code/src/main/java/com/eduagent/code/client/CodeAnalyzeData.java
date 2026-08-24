package com.eduagent.code.client;

import lombok.Data;

import java.util.List;

/**
 * ai-service {@code /code/analyze} 响应 data 段（§1.3.4）。scoreHint 仅参考，最终分由 code-service 裁决。
 */
@Data
public class CodeAnalyzeData {

    private List<Suggestion> suggestions;
    private String summary;
    private String overallComment;
    private Integer scoreHint;

    @Data
    public static class Suggestion {
        private String severity;
        private String location;
        private String title;
        private String detail;
        private String example;
    }
}
