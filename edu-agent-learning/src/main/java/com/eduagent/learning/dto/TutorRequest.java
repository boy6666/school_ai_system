package com.eduagent.learning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 智能辅导对话请求（POST /tutor/chat）。 */
@Data
public class TutorRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;
    private String sessionId;
}
