package com.eduagent.code.client;

import com.eduagent.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 内网 Feign 客户端：调 Python ai-service 的代码质量分析接口（§2.4.5）。
 * <ul>
 *   <li>直连 service 名（docker compose 内网解析，默认，见 §1.5.3 推荐）；可经 {@code edu-agent.ai.base-url} 覆盖</li>
 *   <li>短超时 3s/5s（{@link AiFeignConfig}），绝不让 AI 抖动阻塞判分主线</li>
 * </ul>
 */
@FeignClient(name = "ai-service",
        url = "${edu-agent.ai.base-url:http://ai-service:8001}",
        configuration = AiFeignConfig.class,
        path = "/api/ai")
public interface AiServiceClient {

    @PostMapping("/code/analyze")
    Result<CodeAnalyzeData> analyze(@RequestBody CodeAnalyzeRequest request);
}
