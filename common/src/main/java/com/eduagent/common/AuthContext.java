package com.eduagent.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AuthContext {
    private static final String USER_ID_HEADER = "X-User-Id";

    public static Long getUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest request = attrs.getRequest();
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null) return null;
        try { return Long.parseLong(userId); } catch (NumberFormatException e) { return null; }
    }
}
