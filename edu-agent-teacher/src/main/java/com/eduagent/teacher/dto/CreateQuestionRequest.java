package com.eduagent.teacher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateQuestionRequest {

    @NotBlank(message = "题型不能为空")
    @Pattern(regexp = "choice|code|blank", message = "type 必须为 choice/code/blank")
    private String type;

    @Size(max = 64, message = "章节过长")
    private String chapter;

    @Size(max = 64, message = "知识点过长")
    private String topic;

    @NotBlank(message = "题干不能为空")
    private String content;

    /** 选择题选项（数组）；非 choice 题可空 */
    private List<String> options;

    private String answer;

    private String explanation;

    @Pattern(regexp = "easy|medium|hard", message = "difficulty 必须为 easy/medium/hard")
    private String difficulty = "medium";
}
