package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.dto.LoginRequest;
import com.eduagent.dto.RegisterRequest;
import com.eduagent.entity.User;
import com.eduagent.mapper.UserMapper;
import com.eduagent.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private UserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null) {
            return Result.error("用户不存在");
        }
        String token = tokenProvider.generateToken(user.getId(), request.getUsername());
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return Result.success(result);
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        // 注册逻辑保持不变
        // ...
        return Result.success("注册成功");
    }
}
