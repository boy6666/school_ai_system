package com.eduagent.teacher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级-学生关联实体（纯关联表，复合主键 class_id+student_id）。
 * 不继承 {@link BaseEntity}（无单列自增 id），由 {@code ClassStudentMapper} 的
 * 自定义 SQL 增删查。joinedAt / deleted 由表默认值或自定义 SQL 维护。
 */
@Data
@TableName("class_students")
public class ClassStudent {

    private Long classId;

    private Long studentId;

    private LocalDateTime joinedAt;

    /** 逻辑删除：1=已移除 0=在班 */
    private Integer deleted;
}
