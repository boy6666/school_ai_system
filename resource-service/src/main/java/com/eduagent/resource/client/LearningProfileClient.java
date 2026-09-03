package com.eduagent.resource.client;

import com.eduagent.resource.client.fallback.LearningProfileClientFallback;
import com.eduagent.resource.dto.LearningProfileVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "learning-service",
    url = "${learning.service.url:http://localhost:8082}",
    fallbackFactory = LearningProfileClientFallback.class
)
public interface LearningProfileClient {

    @GetMapping("/api/learning/profile")
    LearningProfileVO getProfile(@RequestHeader("X-User-Id") Long userId);
}
