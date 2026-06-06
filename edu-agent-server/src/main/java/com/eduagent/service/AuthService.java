package com.eduagent.service;

import com.eduagent.dto.LoginRequest;
import com.eduagent.dto.RegisterRequest;
import com.eduagent.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginRequest request);
    LoginVO register(RegisterRequest request);
    LoginVO refreshToken(String refreshToken);
    void logout(Long userId);
    void markOnboarded(Long userId);
}
