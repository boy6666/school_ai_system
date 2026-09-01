package com.eduagent.teacher.feign;

/**
 * 代码判分受理回执（HTTP 202，非全量报告）。status: pending/running/done/failed。
 */
public record CodeSubmitReceiptVO(Long submissionId, String status) {
}
