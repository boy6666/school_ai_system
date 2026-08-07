package com.eduagent.teacher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 出题请求（Feign→ai /resource/generate mode=quiz，返回草稿，教师确认后落库）。
 */
@Data
public class QuestionGenerateRequest {

    @Size(max = 64, message = "章节过长")
    private String chapter;

    @Size(max = 64, message = "知识点过长")
    private String topic;

    @Pattern(regexp = "choice|code|blank", message = "type 必须为 choice/code/blank")
    private String type;

    @Pattern(regexp = "easy|medium|hard", message = "difficulty 必须为 easy/medium/hard")
    private String difficulty = "medium";

    @Min(1)
    @Max(20)
    private Integer count = 5;
}
