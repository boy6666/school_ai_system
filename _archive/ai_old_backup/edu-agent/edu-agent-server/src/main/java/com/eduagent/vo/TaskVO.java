package com.eduagent.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskVO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Integer status;
    private Integer priority;
    private LocalDateTime createTime;
    private Integer duration;      // 新增：预计分钟
    private Integer progress;      // 新增：进度百分比
}
