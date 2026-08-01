package com.eduagent.common.security;

/**
 * 鉴权上下文。下游服务在 Web 过滤器中读取网关注入的 X-User-Id / X-User-Roles 头后写入，
 * 业务代码与 Feign 拦截器据此获取当前用户，请求结束时务必 clear()。
 */
public class AuthContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLES = new ThreadLocal<>();

    public static void set(String userId, String roles) {
        USER_ID.set(userId);
        USER_ROLES.set(roles);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static String getRoles() {
        return USER_ROLES.get();
    }

    public static void clear() {
        USER_ID.remove();
        USER_ROLES.remove();
    }
}
