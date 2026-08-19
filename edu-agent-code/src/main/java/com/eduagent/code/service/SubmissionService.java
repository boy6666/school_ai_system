package com.eduagent.code.service;

import com.eduagent.code.dto.CodeSubmitRequest;
import com.eduagent.code.vo.CodeSubmitReceiptVO;
import com.eduagent.code.vo.CodeSubmitResultVO;

/**
 * 判分提交受理与结果查询（受理口）。
 * 提交仅落库并返回受理回执（202 异步），真正的编译/检查/沙箱/AI/判分由后台 Worker 执行（后续任务）。
 */
public interface SubmissionService {

    /** 接收提交：持久化 submission（status=PENDING）并返回受理回执 */
    CodeSubmitReceiptVO submit(CodeSubmitRequest request);

    /** 查询判分结果（C1：GET /api/code/result/{id}） */
    CodeSubmitResultVO getResult(Long submissionId);
}
