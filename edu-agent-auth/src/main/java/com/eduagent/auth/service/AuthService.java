package com.eduagent.auth.service;

import com.eduagent.auth.dto.LoginRequest;
import com.eduagent.auth.dto.RefreshRequest;
import com.eduagent.auth.dto.RegisterRequest;
import com.eduagent.auth.vo.LoginResponse;
import com.eduagent.auth.vo.UserInfo;

public interface AuthService {

    /** 用户名 + 密码登录，校验通过后签发 JWT。 */
    LoginResponse login(LoginRequest request);

    /** 注册新用户（默认 ROLE_STUDENT），注册成功直接签发 JWT。 */
    LoginResponse register(RegisterRequest request);

    /** 用旧 token 换发新 token（旧 token 需未过期）。 */
    LoginResponse refresh(RefreshRequest request);

    /** 读取当前登录用户信息（网关已注入 X-User-Id）。 */
    UserInfo me();
}
