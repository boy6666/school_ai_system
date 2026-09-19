package com.eduagent.learning.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** edu-agent-ai /path/generate 请求体（§1.3.3）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPathRequest {

    @JsonProperty("studentId")
    private String studentId;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("profile")
    private Map<String, Object> profile;

    /**
     * AI feat/ai 的 path 模型仍读取 snake_case；标准 studentId 保留，合并提供方修复后可移除此别名。
     */
    @JsonProperty("student_id")
    public String getLegacyStudentId() {
        return studentId;
    }
}
