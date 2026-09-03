package com.eduagent.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户。字段对齐旧单体 edu-agent-server 的 users 表（含引导标记 onboarded），
 * 角色为单列 role（student/teacher/admin），由 auth 签发 JWT 时映射为 ROLE_* 写入令牌。
 */
@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private String role;

    private String status;

    /** 引导完成标记：0=未完成，1=已完成（前端 OnboardOverlay 依赖） */
    private Integer onboarded;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
