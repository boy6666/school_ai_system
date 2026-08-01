package com.eduagent.auth.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduagent.auth.entity.Role;
import com.eduagent.auth.entity.User;
import com.eduagent.auth.mapper.RoleMapper;
import com.eduagent.auth.mapper.RoleUserMapper;
import com.eduagent.auth.mapper.UserMapper;
import com.eduagent.common.constant.ServiceConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动幂等初始化。
 * - RBAC 三个角色（ROLE_STUDENT/ROLE_TEACHER/ROLE_ADMIN）为静态参考数据，所有环境（开发/正式）均需要，始终确保存在。
 * - 引导管理员账户在用户表为空时创建一次，口令来自 Nacos/部署环境变量（AUTH_BOOTSTRAP_ADMIN_PASSWORD），正式环境务必覆盖为强口令。
 * 整个流程幂等（已存在即跳过），反复重启不会重复插数据，可安全用于生产启动。
 */
@Component
@DependsOn("flywayInitializer")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final RoleUserMapper roleUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${edu-agent.auth.bootstrap.admin-password:admin123}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        ensureRole(ServiceConstants.ROLE_STUDENT, "学生");
        ensureRole(ServiceConstants.ROLE_TEACHER, "教师");
        ensureRole(ServiceConstants.ROLE_ADMIN, "管理员");

        ensureUser("admin", adminPassword, "系统管理员", ServiceConstants.ROLE_ADMIN);
        ensureUser("teststudent", "student123", "测试学生", ServiceConstants.ROLE_STUDENT);
    }

    private void ensureRole(String code, String name) {
        if (roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, code)) == null) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            roleMapper.insert(role);
        }
    }

    private void ensureUser(String username, String rawPassword, String realName, String roleCode) {
        if (userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)) != null) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setStatus(1);
        userMapper.insert(user);

        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode));
        if (role != null) {
            roleUserMapper.assign(user.getId(), role.getId());
        }
    }
}
