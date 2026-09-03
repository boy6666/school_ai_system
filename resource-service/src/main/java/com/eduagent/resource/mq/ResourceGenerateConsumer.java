package com.eduagent.resource.mq;

import com.eduagent.resource.service.ResourceService;
import com.eduagent.resource.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResourceGenerateConsumer {

    private final ResourceService resourceService;

    @RabbitListener(queues = "resource.generate.queue")
    public void handleGenerate(ResourceGenerateMessage message) {
        log.info("收到生成任务: resourceId={}", message.getResourceId());
        try {
            ResourceVO resource = resourceService.getById(message.getResourceId());
            if (resource == null || !"generating".equals(resource.getStatus())) {
                log.warn("资源不存在或状态不是 generating，跳过: {}", message.getResourceId());
                return;
            }

            String content = resourceService.generateResource(message.getReq());
            // 使用带缓存清除的更新方法
            resourceService.updateResourceContentWithCache(
                    message.getResourceId(),
                    content,
                    "published",
                    resource.getChapterId(),
                    resource.getType()
            );
            log.info("生成完成: resourceId={}", message.getResourceId());
        } catch (Exception e) {
            log.error("生成失败: resourceId={}", message.getResourceId(), e);
            // 获取 resource 信息用于清除缓存
            ResourceVO resource = resourceService.getById(message.getResourceId());
            if (resource != null) {
                resourceService.updateResourceStatusWithCache(
                        message.getResourceId(),
                        "failed",
                        e.getMessage(),
                        resource.getChapterId(),
                        resource.getType()
                );
            } else {
                resourceService.updateResourceStatus(message.getResourceId(), "failed", e.getMessage());
            }
        }
    }
}
