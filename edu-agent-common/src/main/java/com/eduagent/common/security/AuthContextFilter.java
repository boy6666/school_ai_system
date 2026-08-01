package com.eduagent.common.security;

import com.eduagent.common.constant.ServiceConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 鉴权上下文过滤器（框架统一维护，取代各服务重复的 AuthHeaderFilter）。
 *
 * <p>职责：读取网关注入的 X-User-Id / X-User-Roles，写入 {@link AuthContext}，
 * 供业务代码与 {@link AuthFeignInterceptor} 透传使用；请求结束（finally）clear，避免线程复用串号。
 *
 * <p>生效范围：本类位于 common 且被 {@code CommonAutoConfiguration} 的 @ComponentScan 扫到，
 * 所有 servlet 服务（auth/learning/resource/teacher/code）自动获得，无需各自再写。
 * WebFlux 网关不是 servlet 应用，{@code @ConditionalOnWebApplication(SERVLET)} 使其不注册，
 * 既避免重复逻辑，也避免网关引入 servlet 依赖。
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String userId = request.getHeader(ServiceConstants.HEADER_USER_ID);
            String roles = request.getHeader(ServiceConstants.HEADER_USER_ROLES);
            if (userId != null) {
                AuthContext.set(userId, roles);
            }
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }
}
