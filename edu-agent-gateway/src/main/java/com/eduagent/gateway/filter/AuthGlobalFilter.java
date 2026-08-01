package com.eduagent.gateway.filter;

import com.eduagent.common.constant.ServiceConstants;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.result.Result;
import com.eduagent.common.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 网关全局鉴权过滤器（WebFlux 写法，区别于 servlet 服务的 AuthHeaderFilter）。
 * 职责：① 白名单直接放行；② 其余路径校验 Bearer JWT，无效/过期 → 401；
 *       ③ 有效时把 X-User-Id / X-User-Roles 注入下游请求头，供各微服务 AuthContext 读取。
 * 注意：网关是唯一验签点，下游服务信任注入的头、不再重新验签。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> whitelist;

    public AuthGlobalFilter(JwtUtil jwtUtil,
                            @Value("${gateway.auth.whitelist:}") String whitelist) {
        this.jwtUtil = jwtUtil;
        this.whitelist = (whitelist == null || whitelist.isBlank())
                ? List.of() : Arrays.asList(whitelist.split(","));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange);
        String userId;
        String roles;
        try {
            userId = jwtUtil.getUserId(token);
            roles = jwtUtil.getRoles(token);
        } catch (Exception e) {
            return unauthorized(exchange);
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(ServiceConstants.HEADER_USER_ID, userId)
                .header(ServiceConstants.HEADER_USER_ROLES, roles)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhitelisted(String path) {
        return whitelist.stream().anyMatch(path::startsWith);
    }

    private String resolveToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(
                    Result.fail(ErrorCode.UNAUTHORIZED.getCode(), "未认证或令牌无效"));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
