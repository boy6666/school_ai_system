package com.eduagent.teacher.feign;

import com.eduagent.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 代码判分服务 Feign 客户端（路径带 /api/edu-agent-code 前缀）。
 * 异步两段式（C1）：submit 仅返回受理回执 {submissionId,status}（202），
 * 判分结果由 AssignmentGradedConsumer 消费 assignment.graded 事件回填 grades（方案 A，不轮询）。
 * code 服务的 /submit 由吴友诚后续实现；前端 Feign 先按契约定义。
 */
@FeignClient(name = "edu-agent-code", url = "${code.base-url:}",
        path = "/api/edu-agent-code")
public interface CodeServiceClient {

    @PostMapping("/submit")
    Result<CodeSubmitReceiptVO> submit(@RequestBody CodeSubmissionRequest request);
}
