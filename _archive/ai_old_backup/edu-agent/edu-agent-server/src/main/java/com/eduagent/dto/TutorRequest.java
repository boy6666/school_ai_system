package com.eduagent.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TutorRequest {
    @NotBlank(message = "消息不能为空")
    private String message;

    private String sessionId;
}
