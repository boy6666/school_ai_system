package com.eduagent.teacher.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 成绩解读请求。
 */
@Data
public class AiExplainGradeRequest {

    @NotNull(message = "studentId 不能为空")
    private Long studentId;

    @NotNull(message = "assignmentId 不能为空")
    private Long assignmentId;
}
