package com.eduagent.teacher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 建作业请求：写 assignments + assignment_items。
 */
@Data
public class CreateAssignmentRequest {

    @NotNull(message = "classId 不能为空")
    private Long classId;

    @NotBlank(message = "作业标题不能为空")
    @Size(max = 128, message = "标题过长")
    private String title;

    @NotBlank(message = "作业类型不能为空")
    @Pattern(regexp = "homework|code", message = "type 必须为 homework/code")
    private String type;

    private LocalDateTime deadline;

    private String description;

    /** 题目项；建后会为合法 questionId 生成 assignment_items */
    @NotEmpty(message = "作业至少包含一题")
    @Valid
    private List<ItemReq> items;

    @Data
    public static class ItemReq {
        @NotNull(message = "questionId 不能为空")
        private Long questionId;
        private Integer score = 10;
    }
}
