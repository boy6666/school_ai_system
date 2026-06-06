package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.dto.LoginRequest;
import com.eduagent.dto.RegisterRequest;
import com.eduagent.service.AuthService;
import com.eduagent.vo.LoginVO;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return Result.success(authService.refreshToken(token));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        authService.logout(userId);
        return Result.success();
    }

    @PostMapping("/onboard-done")
    public Result<Void> onboardDone() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        authService.markOnboarded(userId);
        return Result.success();
    }
}
