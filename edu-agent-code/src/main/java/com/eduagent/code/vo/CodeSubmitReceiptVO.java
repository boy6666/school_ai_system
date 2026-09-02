package com.eduagent.code.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交受理回执（C1：HTTP 202 异步，仅返回受理号 + 状态，不在此处同步返回全量报告）。
 * status 见 {@link com.eduagent.code.entity.SubmissionStatus}，受理时为 PENDING(0)。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeSubmitReceiptVO {

    private Long submissionId;

    private Integer status;
}
