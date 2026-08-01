package com.eduagent.common.security;

import com.eduagent.common.constant.ServiceConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/**
 * Feign 拦截器：将当前 {@link AuthContext} 中的用户身份透传到下一跳服务。
 * 配合网关注入的 X-User-Id / X-User-Roles（见主蓝图 §9）。由各服务启用
 * {@code @EnableFeignClients} 后自动生效。
 */
@Component
public class AuthFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String userId = AuthContext.getUserId();
        String roles = AuthContext.getRoles();
        if (userId != null) {
            template.header(ServiceConstants.HEADER_USER_ID, userId);
        }
        if (roles != null) {
            template.header(ServiceConstants.HEADER_USER_ROLES, roles);
        }
    }
}
