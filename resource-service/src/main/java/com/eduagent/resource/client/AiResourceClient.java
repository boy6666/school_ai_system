package com.eduagent.resource.client;

import com.eduagent.resource.client.fallback.AiResourceClientFallback;
import com.eduagent.resource.dto.AiPathReq;
import com.eduagent.resource.dto.AiPathResp;
import com.eduagent.resource.dto.AiResourceGenReq;
import com.eduagent.resource.dto.AiResourceGenResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "ai-service",
    url = "${ai.service.url:http://localhost:8001}",
    fallbackFactory = AiResourceClientFallback.class
)
public interface AiResourceClient {

    @PostMapping("/resource/generate")
    AiResourceGenResp generate(@RequestBody AiResourceGenReq req);

    @PostMapping("/path/generate")
    AiPathResp pathGenerate(@RequestBody AiPathReq req);
}
