package com.eduagent.auth.service.impl;

import com.eduagent.auth.dto.LoginRequest;
import com.eduagent.auth.dto.RegisterRequest;
import com.eduagent.auth.entity.User;
import com.eduagent.auth.mapper.UserMapper;
import com.eduagent.auth.service.AuthService;
import com.eduagent.auth.vo.LoginVO;
import com.eduagent.auth.vo.UserInfoVO;
import com.eduagent.common.constant.ServiceConstants;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: username={}", user.getUsername());
        return buildLoginVO(user);
    }

    @Override
    @Transactional
    public LoginVO register(RegisterRequest request) {
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new ApiException(ErrorCode.CONFLICT, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole() != null ? request.getRole() : "student");
        user.setStatus("active");
        user.setOnboarded(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        log.info("用户注册成功: username={}", user.getUsername());
        return buildLoginVO(user);
    }

    @Override
    public LoginVO refreshToken(String token) {
        String raw = token != null && token.startsWith("Bearer ") ? token.substring(7) : token;
        final String userId;
        try {
            userId = jwtUtil.getUserId(raw);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "登录已失效或已过期");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return buildLoginVO(user);
    }

    @Override
    public void logout(Long userId) {
        // JWT 无状态，logout 由前端删除 Token 即可
        log.info("用户登出: userId={}", userId);
    }

    @Override
    public void markOnboarded(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setOnboarded(1);
        userMapper.updateById(user);
        log.info("引导完成: userId={}", userId);
    }

    private LoginVO buildLoginVO(User user) {
        String token = jwtUtil.generateToken(String.valueOf(user.getId()), resolveRole(user.getRole()));
        return LoginVO.builder()
                .token(token)
                .userInfo(toUserInfoVO(user))
                .build();
    }

    /** 把 role 单列映射为网关/契约约定的 ROLE_*，签发进 JWT（AuthGlobalFilter 认证） */
    private String resolveRole(String role) {
        if (role == null) {
            return ServiceConstants.ROLE_STUDENT;
        }
        switch (role) {
            case "teacher":
                return ServiceConstants.ROLE_TEACHER;
            case "admin":
                return ServiceConstants.ROLE_ADMIN;
            case "student":
            default:
                return ServiceConstants.ROLE_STUDENT;
        }
    }

    private UserInfoVO toUserInfoVO(User user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .onboarded(user.getOnboarded())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }
}
