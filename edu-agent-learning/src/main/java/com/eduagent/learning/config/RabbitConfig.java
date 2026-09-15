package com.eduagent.learning.config;

import com.eduagent.common.constant.ServiceConstants;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置。事件风格（决议 C12）：exchange 名 = 事件名，DirectExchange，
 * 发布方声明 exchange（幂等），消费方（edu-agent-teacher）自行绑定时声明队列。
 */
@Configuration
public class RabbitConfig {

    /** study.progress：学情进度事件（learning → teacher 看板消费） */
    @Bean
    public DirectExchange studyProgressExchange() {
        return new DirectExchange(ServiceConstants.EVENT_STUDY_PROGRESS, true, false);
    }

    /** 事件统一 JSON 序列化 */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
