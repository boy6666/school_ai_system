package com.eduagent.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 学习报告。dashboard 的 ai-summary / learning-review 也复用此表存储。 */
@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String title;
    private String content;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String metrics;
    private LocalDateTime createTime;
}
