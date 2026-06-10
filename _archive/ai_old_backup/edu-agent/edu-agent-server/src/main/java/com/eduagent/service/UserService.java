package com.eduagent.service;

import com.eduagent.vo.UserInfoVO;

public interface UserService {
    UserInfoVO getUserInfo(Long userId);
    UserInfoVO updateUserInfo(Long userId, UserInfoVO vo);
}
