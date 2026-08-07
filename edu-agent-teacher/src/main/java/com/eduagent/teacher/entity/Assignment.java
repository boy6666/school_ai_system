package com.eduagent.teacher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduagent.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作业实体。classId 逻辑引用 classes.id；creatorId 逻辑引用 auth_db.users.id。
 * type: homework / code（含代码题）；status: 0草稿 1已发布。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assignments")
public class Assignment extends BaseEntity {

    private Long classId;

    private String title;

    /** homework / code */
    private String type;

    private String description;

    private LocalDateTime deadline;

    /** 0=草稿 1=已发布 */
    private Integer status;

    private Long creatorId;
}
