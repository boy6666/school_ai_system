package com.eduagent.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskUpdateRequest {
    private Long id;
    private Long studentId;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Integer priority;
    private Integer status;
}
