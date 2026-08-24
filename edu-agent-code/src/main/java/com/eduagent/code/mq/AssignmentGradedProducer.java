package com.eduagent.code.mq;

import com.eduagent.code.event.AssignmentGradedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * {@code assignment.graded} 事件发布。判分结果已落库后才发布，teacher 消费事件回填 grades；
 * MQ 不可用时仅记日志（teacher 有轮询兜底路径），绝不因此翻转判分结果。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentGradedProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publish(AssignmentGradedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            rabbitTemplate.send(RabbitConfig.EXCHANGE_ASSIGNMENT_GRADED,
                    RabbitConfig.EXCHANGE_ASSIGNMENT_GRADED,
                    MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                            .setContentType(MediaType.APPLICATION_JSON_VALUE)
                            .build());
        } catch (Exception e) {
            log.warn("assignment.graded 发布失败（teacher 端有轮询兜底），event={}: {}",
                    event.getSubmissionId(), e.toString());
        }
    }
}
