package com.eduagent.resource.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_RESOURCE = "resource.exchange";
    public static final String QUEUE_RESOURCE_GENERATE = "resource.generate.queue";
    public static final String ROUTING_KEY_GENERATE = "resource.generate";

    @Bean
    public TopicExchange resourceExchange() {
        return new TopicExchange(EXCHANGE_RESOURCE);
    }

    @Bean
    public Queue resourceGenerateQueue() {
        return QueueBuilder.durable(QUEUE_RESOURCE_GENERATE)
                .withArgument("x-dead-letter-exchange", EXCHANGE_RESOURCE)
                .withArgument("x-dead-letter-routing-key", "resource.generate.dlq")
                .build();
    }

    @Bean
    public Binding resourceGenerateBinding() {
        return BindingBuilder.bind(resourceGenerateQueue())
                .to(resourceExchange())
                .with(ROUTING_KEY_GENERATE);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
