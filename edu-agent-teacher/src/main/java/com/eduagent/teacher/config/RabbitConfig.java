package com.eduagent.teacher.config;

import com.eduagent.common.constant.ServiceConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：exchange 名 = 事件名（C12），本服务：
 *  发布：assignment.published（→ 学生/前端通知）
 *  消费：assignment.graded（来自 code，回填成绩）、study.progress（来自 learning，刷新看板缓存）
 * 队列加 service 前缀隔离，同一事件不同订阅方各自独立队列。
 */
@Configuration
public class RabbitConfig {

    /** 发布侧 exchange（topic，事件名为 routing key） */
    @Bean
    TopicExchange assignmentPublishedExchange() {
        return new TopicExchange(ServiceConstants.EVENT_ASSIGNMENT_PUBLISHED, true, false);
    }

    /** 消费侧 exchange（来自 code） */
    @Bean
    TopicExchange assignmentGradedExchange() {
        return new TopicExchange(ServiceConstants.EVENT_ASSIGNMENT_GRADED, true, false);
    }

    /** 消费侧 exchange（来自 learning） */
    @Bean
    TopicExchange studyProgressExchange() {
        return new TopicExchange(ServiceConstants.EVENT_STUDY_PROGRESS, true, false);
    }

    @Bean
    Queue assignmentGradedQueue() {
        return new Queue("teacher.assignment.graded.queue", true);
    }

    @Bean
    Queue studyProgressQueue() {
        return new Queue("teacher.study.progress.queue", true);
    }

    @Bean
    Binding assignmentGradedBinding() {
        return BindingBuilder.bind(assignmentGradedQueue())
                .to(assignmentGradedExchange()).with(ServiceConstants.EVENT_ASSIGNMENT_GRADED);
    }

    @Bean
    Binding studyProgressBinding() {
        return BindingBuilder.bind(studyProgressQueue())
                .to(studyProgressExchange()).with(ServiceConstants.EVENT_STUDY_PROGRESS);
    }

    /** 事件体 JSON 序列化（消费者也需反序列化） */
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
