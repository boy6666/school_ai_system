package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.entity.Resource;
import com.eduagent.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    /** 列表查询 */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {

        var pageResult = resourceService.listResources(page, pageSize, keyword, type, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return Result.success(result);
    }

    /** 获取单个资源详情 */
    @GetMapping("/{id}")
    public Result<Resource> getById(@PathVariable Long id) {
        Resource resource = resourceService.getById(id);
        if (resource == null) {
            return Result.error(404, "资源不存在");
        }
        resource.setViews((resource.getViews() != null ? resource.getViews() : 0) + 1);
        resourceService.updateById(resource);
        return Result.success(resource);
    }

    /** 获取章节的所有资源 */
    @GetMapping("/chapter/{chapterId}")
    public Result<List<Resource>> getByChapter(@PathVariable Long chapterId) {
        List<Resource> resources = resourceService.listByChapterId(chapterId);
        return Result.success(resources);
    }

    /** 获取章节的特定类型资源（有则返回缓存，无则AI生成） */
    @GetMapping("/chapter/{chapterId}/{type}")
    public Result<Resource> getChapterResource(
            @PathVariable Long chapterId,
            @PathVariable String type,
            @RequestParam(defaultValue = "medium") String difficulty,
            @RequestParam(defaultValue = "") String chapterName,
            @RequestParam(defaultValue = "") String topic) {

        Long userId = getCurrentUserId();
        Resource resource = resourceService.getByChapterId(chapterId, type);

        if (resource != null && resource.getContent() != null && !resource.getContent().isEmpty()) {
            log.info("命中缓存资源: chapterId={}, type={}, id={}", chapterId, type, resource.getId());
        } else {
            resource = resourceService.generateResource(chapterId, chapterName, topic, type, difficulty, userId);
        }

        return Result.success(resource);
    }

    /** AI生成资源（前端主动触发） */
    @PostMapping("/generate")
    public Result<Resource> generate(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        Long chapterId = body.get("chapterId") != null ? Long.valueOf(body.get("chapterId").toString()) : 0L;
        String chapterName = (String) body.getOrDefault("chapterName", "");
        String topic = (String) body.getOrDefault("topic", chapterName);
        String type = (String) body.getOrDefault("type", "mindmap");
        String difficulty = (String) body.getOrDefault("difficulty", "medium");

        Resource resource = resourceService.generateResource(chapterId, chapterName, topic, type, difficulty, userId);
        return Result.success(resource);
    }

    /** 重新生成资源（换难度） */
    @PostMapping("/{id}/regenerate")
    public Result<Resource> regenerate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        String difficulty = (String) body.getOrDefault("difficulty", "medium");

        Resource resource = resourceService.regenerateResource(id, difficulty, userId);
        return Result.success(resource);
    }

    /** 保存用户反馈 */
    @PostMapping("/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean liked = body.get("liked") != null ? Boolean.valueOf(body.get("liked").toString()) : null;
        String difficultyFeedback = (String) body.getOrDefault("difficulty", "");

        resourceService.saveFeedback(id, liked, difficultyFeedback);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.removeById(id);
        return Result.success();
    }

    private Long getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof Long) return (Long) principal;
            if (principal instanceof String) return Long.parseLong((String) principal);
            return 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}
