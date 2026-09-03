package com.eduagent.auth.service;

import com.eduagent.auth.dto.LoginRequest;
import com.eduagent.auth.dto.RegisterRequest;
import com.eduagent.auth.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginRequest request);

    LoginVO register(RegisterRequest request);

    LoginVO refreshToken(String token);

    void logout(Long userId);

    void markOnboarded(Long userId);
}
