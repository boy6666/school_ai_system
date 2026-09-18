package com.eduagent.learning.dto;

import lombok.Data;

/** 学习路径任务状态更新（PUT /path/task）。 */
@Data
public class UpdateTaskRequest {

    private String stageName;
    private String taskTitle;
    private Boolean completed;
}
