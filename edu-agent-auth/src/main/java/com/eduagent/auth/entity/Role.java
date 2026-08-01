package com.eduagent.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("roles")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码：ROLE_STUDENT / ROLE_TEACHER / ROLE_ADMIN */
    private String code;

    private String name;
}
