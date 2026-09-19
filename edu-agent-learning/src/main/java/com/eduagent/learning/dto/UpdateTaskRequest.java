package com.eduagent.learning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 学习路径任务状态更新（PUT /path/task）。 */
@Data
public class UpdateTaskRequest {

    /** 优先按数据库主键更新；为空时兼容按 stageName + taskTitle 更新。 */
    private Long taskId;
    private String stageName;
    private String taskTitle;

    @NotNull(message = "completed 不能为空")
    private Boolean completed;
}
