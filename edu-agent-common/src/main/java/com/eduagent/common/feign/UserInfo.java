package com.eduagent.common.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 跨服务用户视图（鉴权服务 GET /api/edu-agent-auth/me 的返回值，字段与 auth 的 vo.UserInfo 对齐）。
 * 放在 common 中作为共享契约：任意服务注入 {@link UserClient} 即可拿到调用方身份，
 * 无需各自重复定义 DTO。字段名须与 auth 服务实际返回的 JSON 完全一致（Jackson 按字段名反序列化）。
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
