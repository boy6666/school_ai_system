package com.eduagent.auth.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {

    /** 访问令牌（JWT，网关验签用同一 JwtUtil/secret） */
    private String token;

    private UserInfoVO userInfo;
}
