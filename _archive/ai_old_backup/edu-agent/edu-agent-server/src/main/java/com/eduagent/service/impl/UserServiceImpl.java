package com.eduagent.service.impl;

import com.eduagent.common.BusinessException;
import com.eduagent.entity.User;
import com.eduagent.mapper.UserMapper;
import com.eduagent.service.UserService;
import com.eduagent.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserInfoVO updateUserInfo(Long userId, UserInfoVO vo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (vo.getNickname() != null) user.setNickname(vo.getNickname());
        if (vo.getEmail() != null) user.setEmail(vo.getEmail());
        if (vo.getPhone() != null) user.setPhone(vo.getPhone());
        if (vo.getAvatar() != null) user.setAvatar(vo.getAvatar());
        userMapper.updateById(user);
        return toVO(user);
    }

    private UserInfoVO toVO(User user) {
        return UserInfoVO.builder()
                .id(user.getId()).username(user.getUsername()).nickname(user.getNickname())
                .email(user.getEmail()).phone(user.getPhone()).avatar(user.getAvatar())
                .role(user.getRole()).createTime(user.getCreateTime()).lastLoginTime(user.getLastLoginTime())
                .build();
    }
}
