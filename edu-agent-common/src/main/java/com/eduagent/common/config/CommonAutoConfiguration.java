package com.eduagent.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * common 自动配置。任何依赖 edu-agent-common 的服务启动时会自动扫描
 * com.eduagent.common 下的 {@code @Component}（JwtUtil / AuthFeignInterceptor 等），
 * 无需各服务额外 @ComponentScan。通过 META-INF/spring 的 AutoConfiguration.imports 生效。
 */
@Configuration
@ComponentScan("com.eduagent.common")
public class CommonAutoConfiguration {
}
