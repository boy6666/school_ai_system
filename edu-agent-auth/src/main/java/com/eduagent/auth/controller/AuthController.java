package com.eduagent.auth.controller;

import com.eduagent.auth.dto.LoginRequest;
import com.eduagent.auth.dto.RefreshRequest;
import com.eduagent.auth.dto.RegisterRequest;
import com.eduagent.auth.service.AuthService;
import com.eduagent.auth.vo.LoginResponse;
import com.eduagent.auth.vo.UserInfo;
import com.eduagent.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。网关路由 /api/edu-agent-auth/**（无 StripPrefix），故此处路径即对外完整路径。
 * 登录/注册/刷新在网关白名单内（无需 JWT）；/me 需携带网关校验后注入的 X-User-*。
 */
@RestController
@RequestMapping("/api/edu-agent-auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.success(authService.refresh(request));
    }

    @GetMapping("/me")
    public Result<UserInfo> me() {
        return Result.success(authService.me());
    }
}
