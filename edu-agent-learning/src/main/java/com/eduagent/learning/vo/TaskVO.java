package com.eduagent.learning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 路径任务项。status：0=待开始 1=进行中 2=已完成。 */
@Data
public class TaskVO {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Integer status;
    private Integer priority;
    private LocalDateTime createTime;
    /** 预计分钟 */
    private Integer duration;
    /** 进度百分比 */
    private Integer progress;
}
