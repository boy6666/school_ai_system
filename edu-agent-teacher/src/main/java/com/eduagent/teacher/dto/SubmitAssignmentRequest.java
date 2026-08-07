package com.eduagent.teacher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 学生提交作业请求。studentId 由 AuthContext 取，不信任请求体。
 */
@Data
public class SubmitAssignmentRequest {

    @NotEmpty(message = "提交项不能为空")
    @Valid
    private List<ItemReq> items;

    @Data
    public static class ItemReq {
        @NotNull(message = "itemId 不能为空")
        private Long itemId;
        @NotNull(message = "submission 不能为空")
        private String submission;
        /** code 题语言，如 java */
        @Size(max = 16, message = "语言标识过长")
        private String language;
    }
}
