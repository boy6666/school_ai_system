package com.eduagent.teacher.feign;

import com.eduagent.common.result.Result;
import com.eduagent.teacher.vo.StudentProgressVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 学情服务 Feign 客户端（跨服务调用，路径带 /api/edu-agent-learning 前缀，与网关一致）。
 * url 留空时走 Nacos 服务发现（lb://edu-agent-learning），自动套用 common 的
 * AuthFeignInterceptor 透传教师身份。本接口先声明 Class 模块所需的 bindClass；
 * 看板聚合用的 getProgress/getAnalytics 在 Analytics 模块一并补齐。
 */
@FeignClient(name = "edu-agent-learning", url = "${learning.base-url:}",
        path = "/api/edu-agent-learning")
public interface LearningServiceClient {

    /**
     * 回写学生的班级归属（T 角色可写）。learning 尚未提供该端点时调用会失败，
     * 由调用方（ClassServiceImpl）按 best-effort 捕获，不阻塞本服务。
     */
    @PostMapping("/profile/{studentId}/class")
    Result<Void> bindClass(@PathVariable("studentId") Long studentId,
                           @RequestBody Map<String, Object> body);

    /** 单生学情进度（看板聚合用）。learning 未就绪时调用失败，由 Analytics 兜底。 */
    @GetMapping("/analytics/student/{studentId}/progress")
    Result<StudentProgressVO> getProgress(@PathVariable("studentId") Long studentId);
}
