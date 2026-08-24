package com.eduagent.code.client;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 调用必须短超时（§2.4.5 硬约定）：连接 3s / 读 5s，超时即走 AiFeedbackService 降级。
 */
@Configuration
public class AiFeignConfig {

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(3_000, 5_000);
    }
}
