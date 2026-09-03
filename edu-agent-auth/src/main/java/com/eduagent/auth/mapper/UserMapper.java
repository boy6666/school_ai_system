package com.eduagent.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username} AND status = 'active'")
    User selectByUsername(@Param("username") String username);
}
