package com.eduagent.service;

import com.eduagent.common.Result;
import com.eduagent.dto.LoginRequest;
import com.eduagent.dto.RegisterRequest;
import com.eduagent.entity.User;
import com.eduagent.repository.UserRepository;
import com.eduagent.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Result<Map<String, Object>> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }

        if (user.getStatus() == User.UserStatus.locked) {
            return Result.fail(403, "账户已被锁定");
        }

        if (user.getStatus() == User.UserStatus.inactive) {
            return Result.fail(403, "账户已被禁用");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(),
                user.getRole().name());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("role", user.getRole().name());
        userInfo.put("avatar", user.getAvatar());
        data.put("userInfo", userInfo);

        return Result.ok("登录成功", data);
    }

    public Result<Map<String, Object>> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return Result.fail("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setRole(User.UserRole.valueOf(request.getRole()));
        userRepository.save(user);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());

        return Result.ok("注册成功", data);
    }

    public Result<Map<String, Object>> refresh(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(401, "用户不存在");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(),
                user.getRole().name());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);

        return Result.ok("刷新成功", data);
    }
}
