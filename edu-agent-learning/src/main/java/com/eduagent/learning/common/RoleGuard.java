package com.eduagent.learning.common;

import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.security.AuthContext;

/**
 * 身份与角色守卫。网关只做认证（JWT 验签 + 注入 X-User-Id / X-User-Roles），
 * 角色授权在本服务内完成（见 P0 约定）。common 未提供 requireRole，故此处实现。
 */
public final class RoleGuard {

    private RoleGuard() {
    }

    /** 当前登录用户 id；缺失视为未认证 */
    public static Long currentUserId() {
        String userId = AuthContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED.getCode(), "非法的用户身份: " + userId);
        }
    }

    /** 当前用户 id，未认证返回 null（用于可选登录态场景） */
    public static Long currentUserIdOrNull() {
        try {
            return currentUserId();
        } catch (ApiException e) {
            return null;
        }
    }

    /** 当前用户角色列表（容忍逗号/分号/空格分隔） */
    public static java.util.List<String> currentRoles() {
        String roles = AuthContext.getRoles();
        if (roles == null || roles.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(roles.split("[,;，；\\s]+"))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .toList();
    }

    /** 要求具备任一角色，否则 403 */
    public static void requireAnyRole(String... allowed) {
        currentUserId();
        var own = currentRoles();
        for (String role : allowed) {
            if (own.contains(role)) {
                return;
            }
        }
        throw new ApiException(ErrorCode.FORBIDDEN.getCode(),
                "无权限访问，需要角色: " + String.join("/", allowed));
    }

    /** 教师或管理员 */
    public static void requireTeacherOrAdmin() {
        requireAnyRole(com.eduagent.common.constant.ServiceConstants.ROLE_TEACHER,
                com.eduagent.common.constant.ServiceConstants.ROLE_ADMIN);
    }

    /** 学生（教师/管理员一般不访问学生自助端点，但网关层未限制时放行也无害，按需使用） */
    public static void requireStudent() {
        requireAnyRole(com.eduagent.common.constant.ServiceConstants.ROLE_STUDENT);
    }

    /** 当前学生 id；同时完成登录态和学生角色校验。 */
    public static Long currentStudentId() {
        Long userId = currentUserId();
        requireStudent();
        return userId;
    }
}
