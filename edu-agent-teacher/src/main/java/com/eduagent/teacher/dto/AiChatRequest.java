package com.eduagent.teacher.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * AI 答疑请求（Feign→ai /chat）。
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "message 不能为空")
    private String message;

    /** 可选上下文（如班级学情、题目等），透传给 ai 增强回答 */
    private Map<String, Object> context;
}
