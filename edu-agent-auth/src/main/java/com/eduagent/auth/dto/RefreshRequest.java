package com.eduagent.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * refresh/轮换令牌请求。携带待轮换的 token（登录签发的 JWT，可带 "Bearer " 前缀）。
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "token不能为空")
    private String token;
}
