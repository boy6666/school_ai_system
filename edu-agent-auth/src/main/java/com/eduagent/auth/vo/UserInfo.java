package com.eduagent.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前登录用户信息（GET /api/edu-agent-auth/me 返回）。
 * 角色从 DB 实时读取，保证与 JWT 一致且最新。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private Long userId;

    private String username;

    private String realName;

    private List<String> roles;

    private Integer status;

    private String email;

    private String phone;
}
