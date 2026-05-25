package com.eduagent.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {
    private String message;

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("session_id")
    private String sessionId;
}
