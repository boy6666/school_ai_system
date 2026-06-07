package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.service.UserService;
import com.eduagent.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(userService.getUserInfo(userId));
    }

    @PutMapping("/info")
    public Result<UserInfoVO> updateUserInfo(@RequestBody UserInfoVO vo) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(userService.updateUserInfo(userId, vo));
    }
}
