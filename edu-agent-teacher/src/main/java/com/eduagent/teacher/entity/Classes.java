package com.eduagent.teacher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduagent.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 班级实体。继承 {@link BaseEntity} 后自动获得 id / createTime / updateTime / deleted。
 * teacherId 逻辑引用 auth_db.users.id（无跨库 FK）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classes")
public class Classes extends BaseEntity {

    private String name;

    private Long teacherId;

    private String course;

    private String semester;

    /** 1=启用 0=归档 */
    private Integer status;
}
