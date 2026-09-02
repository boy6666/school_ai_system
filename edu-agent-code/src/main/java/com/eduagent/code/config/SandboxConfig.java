package com.eduagent.code.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Docker 沙箱客户端装配。仅 {@code edu-agent.judge.sandbox=docker} 时创建
 * （docker-java 3.3.x 无 DockerClientBuilder，入口为 DockerClientImpl.getInstance(config)）。
 * URI 按部署环境配置：Linux 默认 unix socket，Windows Docker Desktop 用 npipe:////./pipe/docker_engine。
 */
@Configuration
public class SandboxConfig {

    @Bean
    @ConditionalOnProperty(name = "edu-agent.judge.sandbox", havingValue = "docker")
    public DockerClient dockerClient(@Value("${edu-agent.judge.docker-uri:unix:///var/run/docker.sock}") String uri) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(uri)
                .build();
        return DockerClientImpl.getInstance(config);
    }
}
