package com.eduagent.teacher.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 教师复核成绩请求：仅覆盖非空字段。
 */
@Data
public class UpdateGradeRequest {

    private Integer score;

    @Size(max = 2000, message = "评语过长")
    private String comment;

    /** 覆盖 AI 建议（JSON 字符串） */
    private String aiReportOverride;
}
