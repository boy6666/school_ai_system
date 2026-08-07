package com.eduagent.teacher.mq;

import com.eduagent.common.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作业发布事件（exchange=routingKey=assignment.published）。
 * 发布供学生通知/前端轮询消费（当前无独立通知服务，先落事件）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssignmentPublishedEvent extends BaseEvent {

    public static final String TOPIC = "assignment.published";

    private Long assignmentId;
    private Long classId;
    private String title;
    private String type;
    private LocalDateTime deadline;
}
