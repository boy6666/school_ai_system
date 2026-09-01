package com.eduagent.teacher.feign;

import com.eduagent.common.result.Result;
import com.eduagent.teacher.dto.AiChatRequest;
import com.eduagent.teacher.dto.AiResourceRequest;
import com.eduagent.teacher.vo.AiChatResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * AI 服务 Feign 客户端（路径带 /api/edu-agent-ai 前缀，与网关一致）。
 * 注意：ai 为 Python 且当前根路径挂载（/chat 等无前缀、未走 Nacos lb），
 * 联调时以网关实际配置为准（§B 路由表已注明现实）；此处按目标契约定义。
 */
@FeignClient(name = "edu-agent-ai", url = "${ai.base-url:}",
        path = "/api/edu-agent-ai")
public interface AiServiceClient {

    /** AI 助教答疑 */
    @PostMapping("/chat")
    Result<AiChatResult> chat(@RequestBody AiChatRequest request);

    /** 内容生成（出题/评价等），mode 字段决定返回结构，按 mode 由调用方解析 */
    @PostMapping("/resource/generate")
    Result<Map<String, Object>> generate(@RequestBody AiResourceRequest request);
}
