package com.eduagent.code.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduagent.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码练习实体。继承 {@link BaseEntity} 后自动获得 id / createTime / updateTime / deleted
 * （均由框架在插入、更新时填充/管理，业务层无需赋值）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("code_exercises")
public class CodeExercise extends BaseEntity {

    private String title;

    private String description;

    /** EASY / MEDIUM / HARD */
    private String difficulty;

    private String language;

    /** 1=启用 0=禁用 */
    private Integer status;
}
