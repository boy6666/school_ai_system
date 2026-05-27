package com.eduagent.security;

public class SecurityUtils {
    public static Long getCurrentUserId() {
        Long userId = UserIdHolder.getCurrentUserId();
        if (userId == null) {
            // fallback: 从 SecurityContext 获取，但需要扩展 UserDetails
            return 1L;
        }
        return userId;
    }
}
