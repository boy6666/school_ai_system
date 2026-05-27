package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.entity.User;
import com.eduagent.mapper.UserMapper;
import com.eduagent.security.SecurityUtils;
import com.eduagent.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    public Result<UserInfoVO> getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRole(user.getRole());   // 添加这一行
        vo.setName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        return Result.success(vo);
    }

    @PutMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateUserInfo(@RequestBody Map<String, String> updates) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        if (updates.containsKey("nickname")) {
            user.setNickname(updates.get("nickname"));
        }
        if (updates.containsKey("email")) {
            user.setEmail(updates.get("email"));
        }
        userMapper.updateById(user);
        return Result.success(null);
    }
}
