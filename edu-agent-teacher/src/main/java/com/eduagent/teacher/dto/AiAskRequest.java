package com.eduagent.teacher.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 教师 AI 助教提问请求。
 */
@Data
public class AiAskRequest {

    @NotBlank(message = "message 不能为空")
    private String message;

    private Long classId;

    private Map<String, Object> context;
}
