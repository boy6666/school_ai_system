package com.eduagent.agent;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class AiConfig {
    @Value("${ai.base-url:http://localhost:8000}")
    private String baseUrl;
}
