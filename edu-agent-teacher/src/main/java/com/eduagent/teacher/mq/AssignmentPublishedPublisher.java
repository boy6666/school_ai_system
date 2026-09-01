package com.eduagent.teacher.mq;

import com.eduagent.common.constant.ServiceConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 作业发布事件发布器。exchange 名 = 事件名（对齐《契约对齐决议》C12）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentPublishedPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(AssignmentPublishedEvent event) {
        try {
            rabbitTemplate.convertAndSend(ServiceConstants.EVENT_ASSIGNMENT_PUBLISHED,
                    ServiceConstants.EVENT_ASSIGNMENT_PUBLISHED, event);
            log.info("[AssignmentPublished] assignmentId={} classId={}", event.getAssignmentId(), event.getClassId());
        } catch (Exception e) {
            // 发布失败不阻断主流程：作业已入库，通知可由下次 publish 重发。
            log.error("[AssignmentPublished] 发送失败 assignmentId={}: {}",
                    event.getAssignmentId(), e.getMessage());
        }
    }
}
