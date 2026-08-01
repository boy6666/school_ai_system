package com.eduagent.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduagent.auth.dto.LoginRequest;
import com.eduagent.auth.dto.RefreshRequest;
import com.eduagent.auth.dto.RegisterRequest;
import com.eduagent.auth.entity.Role;
import com.eduagent.auth.entity.User;
import com.eduagent.auth.mapper.RoleMapper;
import com.eduagent.auth.mapper.RoleUserMapper;
import com.eduagent.auth.mapper.UserMapper;
import com.eduagent.auth.security.UserDetailsServiceImpl;
import com.eduagent.auth.service.AuthService;
import com.eduagent.auth.vo.LoginResponse;
import com.eduagent.auth.vo.UserInfo;
import com.eduagent.common.constant.ServiceConstants;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.security.AuthContext;
import com.eduagent.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final RoleUserMapper roleUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        List<String> roles = roleMapper.selectCodesByUserId(user.getId());
        String token = jwtUtil.generateToken(String.valueOf(user.getId()), String.join(",", roles));
        return new LoginResponse(token, user.getId(), roles, user.getRealName());
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername())) != null) {
            throw new ApiException(ErrorCode.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        userMapper.insert(user);

        String roleCode = resolveRoleCode(request.getRole());
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode));
        if (role != null) {
            roleUserMapper.assign(user.getId(), role.getId());
        }
        List<String> roles = List.of(roleCode);
        String token = jwtUtil.generateToken(String.valueOf(user.getId()), String.join(",", roles));
        return new LoginResponse(token, user.getId(), roles, user.getRealName());
    }

    @Override
    public LoginResponse refresh(RefreshRequest request) {
        String token = request.getToken();
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String userId;
        String rolesStr;
        try {
            userId = jwtUtil.getUserId(token);
            rolesStr = jwtUtil.getRoles(token);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "令牌无效或已过期");
        }
        List<String> roles = (rolesStr == null || rolesStr.isBlank())
                ? List.of()
                : Arrays.asList(rolesStr.split(","));
        User user = userMapper.selectById(Long.parseLong(userId));
        if (user == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        String newToken = jwtUtil.generateToken(userId, String.join(",", roles));
        return new LoginResponse(newToken, user.getId(), roles, user.getRealName());
    }

    @Override
    public UserInfo me() {
        String userIdStr = AuthContext.getUserId();
        if (userIdStr == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "未认证");
        }
        User user = userMapper.selectById(Long.parseLong(userIdStr));
        if (user == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        List<String> roles = roleMapper.selectCodesByUserId(user.getId());
        return new UserInfo(user.getId(), user.getUsername(), user.getRealName(), roles,
                user.getStatus(), user.getEmail(), user.getPhone());
    }

    /** 归一化角色编码：student/admin/teacher 或已带 ROLE_ 前缀，均映射为合法的 ROLE_xxx，缺省学生。 */
    private String resolveRoleCode(String role) {
        if (role == null || role.isBlank()) {
            return ServiceConstants.ROLE_STUDENT;
        }
        String r = role.trim().toUpperCase();
        if (!r.startsWith("ROLE_")) {
            r = "ROLE_" + r;
        }
        return switch (r) {
            case ServiceConstants.ROLE_ADMIN, ServiceConstants.ROLE_TEACHER, ServiceConstants.ROLE_STUDENT -> r;
            default -> ServiceConstants.ROLE_STUDENT;
        };
    }
}
