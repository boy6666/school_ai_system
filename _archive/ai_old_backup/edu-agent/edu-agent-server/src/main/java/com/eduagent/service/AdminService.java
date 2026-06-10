package com.eduagent.service;

import com.eduagent.common.PageResult;
import com.eduagent.vo.UserInfoVO;
import java.util.Map;

public interface AdminService {

    PageResult<UserInfoVO> listUsers(int page, int pageSize, String keyword);
    UserInfoVO updateUserRole(Long userId, String role);
    void deleteUser(Long userId);
    Map<String, Object> getStats();
}
