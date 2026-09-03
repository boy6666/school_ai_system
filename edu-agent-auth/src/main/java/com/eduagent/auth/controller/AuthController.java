package com.eduagent.auth.controller;

import com.eduagent.auth.dto.LoginRequest;
import com.eduagent.auth.dto.RefreshRequest;
import com.eduagent.auth.dto.RegisterRequest;
import com.eduagent.auth.service.AuthService;
import com.eduagent.auth.vo.LoginVO;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.result.Result;
import com.eduagent.common.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/edu-agent-auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录（网关白名单，匿名可访问） */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /** 注册（网关白名单，匿名可访问） */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /** 令牌轮换（需携带有效 token 通过网关验签） */
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.success(authService.refreshToken(request.getToken()));
    }

    /** 登出（JWT 无状态，服务端仅记录；前端删 Token 即可） */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(currentUserId());
        return Result.success();
    }

    /** 标记引导完成（前端 OnboardOverlay 依赖） */
    @PostMapping("/onboard-done")
    public Result<Void> onboardDone() {
        authService.markOnboarded(currentUserId());
        return Result.success();
    }

    private Long currentUserId() {
        String userId = AuthContext.getUserId();
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "未认证");
        }
        return Long.valueOf(userId);
    }
}
