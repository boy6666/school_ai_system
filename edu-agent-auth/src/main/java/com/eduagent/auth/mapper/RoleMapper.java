package com.eduagent.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /** 根据用户 id 查询其拥有的角色编码（ROLE_xxx），用于登录下发 JWT 与 /me 响应。 */
    @Select("SELECT r.code FROM roles r " +
            "JOIN role_user ru ON r.id = ru.role_id " +
            "WHERE ru.user_id = #{userId}")
    List<String> selectCodesByUserId(@Param("userId") Long userId);
}
