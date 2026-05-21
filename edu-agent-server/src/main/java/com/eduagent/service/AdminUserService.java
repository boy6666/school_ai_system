package com.eduagent.service;

import com.eduagent.dto.CreateUserRequest;
import com.eduagent.dto.UpdateUserRequest;
import com.eduagent.entity.User;
import com.eduagent.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<User> getUserList(String keyword, String role, String status, int page, int pageSize) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), kw),
                        cb.like(root.get("nickname"), kw),
                        cb.like(root.get("email"), kw)
                ));
            }
            if (StringUtils.hasText(role)) {
                predicates.add(cb.equal(root.get("role"), User.UserRole.valueOf(role)));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), User.UserStatus.valueOf(status)));
            }

            query.orderBy(cb.desc(root.get("createTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, PageRequest.of(page - 1, pageSize));
    }

    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setRole(User.UserRole.valueOf(request.getRole()));
        return userRepository.save(user);
    }

    public User updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (StringUtils.hasText(request.getNickname())) user.setNickname(request.getNickname());
        if (StringUtils.hasText(request.getEmail())) user.setEmail(request.getEmail());
        if (StringUtils.hasText(request.getPhone())) user.setPhone(request.getPhone());
        if (StringUtils.hasText(request.getRole())) user.setRole(User.UserRole.valueOf(request.getRole()));
        if (StringUtils.hasText(request.getStatus())) user.setStatus(User.UserStatus.valueOf(request.getStatus()));

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    public User toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getStatus() == User.UserStatus.active) {
            user.setStatus(User.UserStatus.inactive);
        } else {
            user.setStatus(User.UserStatus.active);
        }

        return userRepository.save(user);
    }

    public Map<String, Object> toUserVO(User user) {
        Map<String, Object> vo = new HashMap<>();
        vo.put("id", user.getId());
        vo.put("username", user.getUsername());
        vo.put("nickname", user.getNickname());
        vo.put("email", user.getEmail());
        vo.put("phone", user.getPhone());
        vo.put("avatar", user.getAvatar());
        vo.put("role", user.getRole().name());
        vo.put("status", user.getStatus().name());
        vo.put("createTime", user.getCreateTime());
        vo.put("lastLoginTime", user.getLastLoginTime());
        return vo;
    }
}
