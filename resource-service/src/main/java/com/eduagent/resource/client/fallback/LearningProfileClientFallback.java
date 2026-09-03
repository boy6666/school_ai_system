package com.eduagent.resource.client.fallback;

import com.eduagent.resource.client.LearningProfileClient;
import com.eduagent.resource.dto.LearningProfileVO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class LearningProfileClientFallback implements FallbackFactory<LearningProfileClient> {

    @Override
    public LearningProfileClient create(Throwable cause) {
        return userId -> {
            // 画像服务不可用时返回 null，业务层做降级处理
            return null;
        };
    }
}
