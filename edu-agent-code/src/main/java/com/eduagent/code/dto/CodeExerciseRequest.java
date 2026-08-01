package com.eduagent.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CodeExerciseRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题过长")
    private String title;

    private String description;

    private String difficulty = "EASY";

    private String language = "java";
}
