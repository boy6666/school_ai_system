package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.common.BusinessException;
import com.eduagent.common.PageResult;
import com.eduagent.entity.User;
import com.eduagent.mapper.ConversationMapper;
import com.eduagent.mapper.UserMapper;
import com.eduagent.service.AdminService;
import com.eduagent.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final ConversationMapper conversationMapper;

    @Override
    public PageResult<UserInfoVO> listUsers(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> result = userMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<UserInfoVO> vos = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(vos, result.getTotal(), page, pageSize);
    }

    @Override
    public UserInfoVO updateUserRole(Long userId, String role) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setRole(role);
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public void deleteUser(Long userId) {
        if (userMapper.deleteById(userId) <= 0) {
            throw new BusinessException(404, "用户不存在");
        }
    }

    @Override
    public Map<String, Object> getStats() {
        Long totalUsers = userMapper.selectCount(null);
        Long studentCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "student"));
        Long adminCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "admin"));
        Long totalConversations = conversationMapper.selectCount(null);
        Long todayConversations = conversationMapper.selectCount(
            new LambdaQueryWrapper<com.eduagent.entity.Conversation>()
                .ge(com.eduagent.entity.Conversation::getCreateTime, java.time.LocalDate.now().atStartOfDay())
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", totalUsers);
        stats.put("studentCount", studentCount);
        stats.put("adminCount", adminCount);
        stats.put("totalConversations", totalConversations);
        stats.put("todayConversations", todayConversations);
        return stats;
    }

    private UserInfoVO toVO(User user) {
        return UserInfoVO.builder()
                .id(user.getId()).username(user.getUsername()).nickname(user.getNickname())
                .email(user.getEmail()).phone(user.getPhone()).avatar(user.getAvatar())
                .role(user.getRole()).status(user.getStatus()).createTime(user.getCreateTime()).lastLoginTime(user.getLastLoginTime())
                .build();
    }
}
