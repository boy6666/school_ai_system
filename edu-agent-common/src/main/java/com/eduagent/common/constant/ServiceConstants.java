package com.eduagent.common.constant;

/**
 * 跨服务常量：服务名、Nacos group、角色、透传头、MQ 事件名。
 * 统一在此定义，避免各服务硬编码导致漂移（呼应《契约对齐决议》C11/C12）。
 */
public final class ServiceConstants {

    private ServiceConstants() {
    }

    /** 服务名（Nacos 注册名 + Feign 目标名，路径统一带 /api/&lt;svc&gt;） */
    public static final String SVC_GATEWAY = "edu-agent-gateway";
    public static final String SVC_AUTH = "edu-agent-auth";
    public static final String SVC_LEARNING = "edu-agent-learning";
    public static final String SVC_RESOURCE = "edu-agent-resource";
    public static final String SVC_TEACHER = "edu-agent-teacher";
    public static final String SVC_CODE = "edu-agent-code";

    /** Nacos group（C11：kebab 风格统一） */
    public static final String GROUP_DEFAULT = "edu-agent";
    public static final String GROUP_RESOURCE = "resource-group";

    /** 角色（auth 签发 JWT 时写入，网关透传） */
    public static final String ROLE_STUDENT = "ROLE_STUDENT";
    public static final String ROLE_TEACHER = "ROLE_TEACHER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /** 网关注入、Feign 透传的身份头 */
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    /** MQ 事件名（C12：exchange 名 = 事件名） */
    public static final String EVENT_STUDY_PROGRESS = "study.progress";
    public static final String EVENT_ASSIGNMENT_PUBLISHED = "assignment.published";
    public static final String EVENT_ASSIGNMENT_GRADED = "assignment.graded";
    public static final String EVENT_RESOURCE_GENERATE = "resource.generate";
}
