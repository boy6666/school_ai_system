package com.eduagent.resource.controller;

import com.eduagent.common.Result;
import com.eduagent.resource.dto.FeedbackReq;
import com.eduagent.resource.dto.ResourceGenerateReq;
import com.eduagent.resource.service.ResourceService;
import com.eduagent.resource.vo.ResourceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/edu-agent-resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    // ========== 查询 ==========

    @GetMapping
    public Result<List<ResourceVO>> list() {
        return Result.success(resourceService.listAll());
    }

    @GetMapping("/{id}")
    public Result<ResourceVO> getById(@PathVariable Long id) {
        ResourceVO vo = resourceService.getById(id);
        if (vo == null) {
            return Result.error(404, "资源不存在");
        }
        return Result.success(vo);
    }

    @GetMapping("/chapter/{chapterId}")
    public Result<List<ResourceVO>> listByChapter(@PathVariable String chapterId) {
        return Result.success(resourceService.listByChapter(chapterId));
    }

    @GetMapping("/chapter/{chapterId}/{type}")
    public Result<List<ResourceVO>> listByChapterAndType(@PathVariable String chapterId, @PathVariable String type) {
        return Result.success(resourceService.listByChapterAndType(chapterId, type));
    }

    @GetMapping("/favorites/mine")
    public Result<List<ResourceVO>> myFavorites() {
        return Result.success(resourceService.myFavorites());
    }

    // ========== 创建 ==========

    @PostMapping
    public Result<Void> create(@RequestBody ResourceVO resource) {
        resourceService.create(resource);
        return Result.success();
    }

    // ========== 更新 ==========

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<Result<ResourceVO>> regenerate(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {
        String difficulty = payload != null ? payload.get("difficulty") : null;
        ResourceVO resource = resourceService.regenerate(id, difficulty);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Result.success(resource));
    }

    // ========== 删除 ==========

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return Result.success();
    }

    // ========== 收藏 ==========

    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id, @RequestParam boolean favorite) {
        resourceService.favorite(id, favorite);
        return Result.success();
    }

    // ========== 反馈 ==========

    @PostMapping("/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody FeedbackReq req) {
        resourceService.feedback(id, req);
        return Result.success();
    }

    // ========== AI 生成（异步） ==========

    @PostMapping("/generate")
    public ResponseEntity<Result<ResourceVO>> generate(@RequestBody ResourceGenerateReq req) {
        if (req.getUserId() == null || req.getTopic() == null || req.getType() == null) {
            return ResponseEntity.badRequest().body(Result.error(400, "userId, topic, type 为必填字段"));
        }
        ResourceVO resource = resourceService.generateAsync(req);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Result.success(resource));
    }
}
