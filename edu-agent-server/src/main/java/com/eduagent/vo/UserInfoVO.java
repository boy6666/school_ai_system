package com.eduagent.vo;

import lombok.Data;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String name;
    private String nickname;
    private String email;
    private String avatar;
    private String role;
}
