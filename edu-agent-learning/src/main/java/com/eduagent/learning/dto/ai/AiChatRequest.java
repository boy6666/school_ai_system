package com.eduagent.learning.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** edu-agent-ai /chat 请求体（§1.3.1，camelCase）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @JsonProperty("userInput")
    private String userInput;

    @JsonProperty("studentId")
    private String studentId;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("profile")
    private java.util.Map<String, Object> profile;
}
