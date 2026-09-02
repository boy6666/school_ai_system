package com.eduagent.code.mq;

import com.eduagent.common.constant.ServiceConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 出栈事件 RabbitMQ 装配（C12：exchange 名 = 事件名）。
 * 队列与绑定由消费方声明（teacher 消费 {@code teacher.assignment.graded.queue} 绑该 exchange）。
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_ASSIGNMENT_GRADED = ServiceConstants.EVENT_ASSIGNMENT_GRADED;

    @Bean
    public TopicExchange assignmentGradedExchange() {
        return new TopicExchange(EXCHANGE_ASSIGNMENT_GRADED, true, false);
    }
}
