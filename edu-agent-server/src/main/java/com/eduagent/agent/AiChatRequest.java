package com.eduagent.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {
    @JsonProperty("user_input")
    private String message;

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("profile")
    private Map<String, Object> profile;
}
