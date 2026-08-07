package com.eduagent.teacher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduagent.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 作业题目项实体。assignmentId 逻辑引用 assignments.id；questionId 逻辑引用 questions.id。
 * itemType: choice / code / blank。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assignment_items")
public class AssignmentItem extends BaseEntity {

    private Long assignmentId;

    private Long questionId;

    /** choice / code / blank */
    private String itemType;

    /** 该项满分 */
    private Integer score;

    /** 顺序 */
    private Integer orderNum;
}
