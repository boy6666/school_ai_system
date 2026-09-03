package com.eduagent.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private String role;

    /** 引导完成标记（0/1） */
    private Integer onboarded;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;
}
