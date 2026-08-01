package com.eduagent.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录/注册成功返回体（p0 §4.3：{ token, userId, roles, realName }）。
 * token 为 Bearer JWT，roles 为 ROLE_xxx 列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private Long userId;

    private List<String> roles;

    private String realName;
}
