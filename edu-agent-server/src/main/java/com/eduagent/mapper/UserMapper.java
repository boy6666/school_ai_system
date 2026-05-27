package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
