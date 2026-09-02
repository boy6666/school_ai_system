package com.eduagent.code.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduagent.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码检查/判分报告实体（dev-wuyoucheng §2.2）。
 * checkstyle / pmd / scoreDetail 均以 JSON 文本存列、以 String 承载。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("code_check_reports")
public class CodeCheckReport extends BaseEntity {

    private Long submissionId;

    /** 0=未过 1=通过 */
    private Integer compileOk;

    private String compileMsg;

    private String checkstyle;

    private String pmd;

    private String aiSuggestion;

    private Integer overallScore;

    private String scoreDetail;
}
