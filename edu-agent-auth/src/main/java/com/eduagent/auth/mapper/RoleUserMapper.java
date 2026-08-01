package com.eduagent.auth.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联表（role_user）的纯注解 Mapper。
 * 该表为联合主键的中间表，无需 MyBatis-Plus 实体，直接用原生 SQL 维护。
 */
@Mapper
public interface RoleUserMapper {

    @Insert("INSERT IGNORE INTO role_user(user_id, role_id) VALUES(#{userId}, #{roleId})")
    void assign(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT role_id FROM role_user WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
