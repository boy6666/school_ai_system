package com.eduagent.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
