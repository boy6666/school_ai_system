package com.eduagent.learning.feign;

import com.eduagent.learning.dto.ai.AiChatRequest;
import com.eduagent.learning.dto.ai.AiPathRequest;
import com.eduagent.learning.dto.ai.AiResourceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI 服务客户端（Python，Nacos 名 edu-agent-ai）。
 * 路径统一带 /api/edu-agent-ai 前缀（与网关转发路径一致，网关不 StripPrefix，见 §1.5.1）。
 * url 默认空 → 走 Nacos 服务发现；配置 ai.base-url 后兜底直连。
 * 身份透传由 common 的 AuthFeignInterceptor 自动完成。
 */
@FeignClient(name = "edu-agent-ai", url = "${ai.base-url:}", path = "/api/edu-agent-ai")
public interface AiServiceClient {

    @PostMapping("/chat")
    String chat(@RequestBody AiChatRequest request);

    @PostMapping("/path/generate")
    String generatePath(@RequestBody AiPathRequest request);

    @PostMapping("/resource/generate")
    String generateResource(@RequestBody AiResourceRequest request);
}
