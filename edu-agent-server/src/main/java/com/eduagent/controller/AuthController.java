package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.dto.LoginRequest;
import com.eduagent.dto.RegisterRequest;
import com.eduagent.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.ok("登出成功");
    }

    @PostMapping("/refresh")
    public Result<?> refresh(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return authService.refresh(userId);
    }
}
