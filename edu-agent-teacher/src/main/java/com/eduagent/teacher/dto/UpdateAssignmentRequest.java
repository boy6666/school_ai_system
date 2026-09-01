package com.eduagent.teacher.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业部分更新：仅覆盖非空字段（title/deadline/status）。
 */
@Data
public class UpdateAssignmentRequest {

    @Size(max = 128, message = "标题过长")
    private String title;

    private LocalDateTime deadline;

    @Pattern(regexp = "0|1", message = "status 必须为 0/1")
    private String status;
}
