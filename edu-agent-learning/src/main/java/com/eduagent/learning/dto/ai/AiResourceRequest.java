package com.eduagent.learning.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * edu-agent-ai /resource/generate 请求体（§1.3.2 + 决议 C3）。
 * mode 取值：resource（默认）/ judge / suggestion；响应结构随 mode 不同。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResourceRequest {

    @JsonProperty("studentId")
    private String studentId;

    @JsonProperty("chapter")
    private String chapter;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("resourceType")
    private String resourceType;

    /** basic / intermediate / advanced（映射原 difficulty） */
    @JsonProperty("level")
    private String level;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("profile")
    private Map<String, Object> profile;
}
