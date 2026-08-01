package com.eduagent.code.security;

import com.eduagent.common.constant.ServiceConstants;
import com.eduagent.common.security.AuthContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 读取网关注入的 X-User-Id / X-User-Roles，写入 AuthContext 供业务与 Feign 透传使用。
 * 请求结束（finally）务必 clear，避免线程复用串号。
 */
@Component
public class AuthHeaderFilter extends OncePerRequestFilter {

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
