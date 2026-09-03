package com.eduagent.resource.client.fallback;

import com.eduagent.common.exception.BusinessException;
import com.eduagent.resource.client.AiResourceClient;
import com.eduagent.resource.dto.AiPathReq;
import com.eduagent.resource.dto.AiPathResp;
import com.eduagent.resource.dto.AiResourceGenReq;
import com.eduagent.resource.dto.AiResourceGenResp;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AiResourceClientFallback implements FallbackFactory<AiResourceClient> {

    @Override
    public AiResourceClient create(Throwable cause) {
        return new AiResourceClient() {
            @Override
            public AiResourceGenResp generate(AiResourceGenReq req) {
                throw new BusinessException(502, "AI 服务不可用，资源生成失败");
            }

            @Override
            public AiPathResp pathGenerate(AiPathReq req) {
                throw new BusinessException(502, "AI 服务不可用，路径生成失败");
            }
        };
    }
}
