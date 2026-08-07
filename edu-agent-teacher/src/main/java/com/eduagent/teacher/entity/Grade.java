package com.eduagent.teacher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduagent.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 成绩/批改实体（核心表）。runResult / staticReport / aiReport 为 MySQL JSON 列，
 * Java 侧用 String 承载，VO 层解析。status: 0待批 1已批。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("grades")
public class Grade extends BaseEntity {

    private Long assignmentId;

    private Long studentId;

    private Long itemId;

    /** choice / code / blank */
    private String itemType;

    /** code 题语言，如 java */
    private String language;

    /** 学生提交内容/代码 */
    private String submission;

    /** 运行结果 JSON（来自 edu-agent-code） */
    private String runResult;

    /** 静态检查报告 JSON（来自 edu-agent-code） */
    private String staticReport;

    /** AI 建议 JSON（来自 edu-agent-code / ai） */
    private String aiReport;

    private Integer score;

    /** 0=待批 1=已批 */
    private Integer status;

    /** 教师评语/复核 */
    private String comment;

    private LocalDateTime gradedAt;
}
