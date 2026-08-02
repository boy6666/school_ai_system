package com.eduagent.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 所有数据库实体统一继承的基类（审计字段 + 主键 + 逻辑删除）。
 *
 * <ul>
 *   <li>{@code id}：自增主键（各表主键列名即为 id）。</li>
 *   <li>{@code createTime / updateTime}：由 {@code AutoFillMetaObjectHandler} 在插入/更新时自动填充，实体无需手动赋值。</li>
 *   <li>{@code deleted}：逻辑删除，依赖各服务 Nacos 中
 *       {@code mybatis-plus.global-config.db-config.logic-delete-field=deleted} 全局生效。
 *       <b>因此继承本类的实体，其数据表必须含 {@code deleted} 列</b>（见各服务 Flyway 迁移脚本，如 code 的 V2）。</li>
 * </ul>
 *
 * 用法：新建实体直接 {@code extends BaseEntity}，仅声明业务字段即可。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=未删 1=已删。列名由各服务 mybatis-plus 全局配置映射，无需在此加 @TableLogic。 */
    private Integer deleted;
}
