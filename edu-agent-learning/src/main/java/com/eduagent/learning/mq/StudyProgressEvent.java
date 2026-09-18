package com.eduagent.learning.mq;

import com.eduagent.common.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 学情进度事件（study.progress，供 edu-agent-teacher 看板消费）。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StudyProgressEvent extends BaseEvent {

    private Long studentId;
    /** 来自 profile.class_id（teacher_db 逻辑引用，无外键） */
    private Long classId;
    /** 路径整体进度 % */
    private Integer progress;
    private Integer completedTasks;
    private Integer totalTasks;
    /** 六维平均掌握度 % */
    private Integer masteryRate;
    private LocalDateTime eventTime;
}
