package com.eduagent.code.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduagent.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码提交实体（dev-wuyoucheng §2.2）。继承 {@link BaseEntity} 自动获得 id / createTime / updateTime / deleted。
 * status 见 {@link com.eduagent.code.entity.SubmissionStatus}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("code_submissions")
public class CodeSubmission extends BaseEntity {

    private Long studentId;

    /** 作业 id（逻辑引用 teacher_db，用于 assignment.graded 事件回填关联） */
    private Long assignmentId;

    private Long assignmentItemId;

    private String language;

    private String className;

    /** 期望输出，参与判分权重 */
    private String expectedOutput;

    private String sourceCode;

    /** 判分状态：0=待处理 1=运行中 2=已完成 3=超时 4=编译失败 5=判分失败 */
    private Integer status;

    private String stdout;

    private String stderr;

    private Integer runTimeMs;
}
