package com.eduagent.resource.controller;

import com.eduagent.common.Result;
import com.eduagent.resource.dto.KbImportReq;
import com.eduagent.resource.dto.KbStatsResp;
import com.eduagent.resource.dto.MarkIndexedReq;
import com.eduagent.resource.service.KbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kb")
@RequiredArgsConstructor
public class KbController {

    private final KbService kbService;

    // 按状态查询语料（支持数字映射）
    @GetMapping("/corpus")
    public Result<List<Map<String, Object>>> getCorpusByStatus(@RequestParam String status) {
        return Result.success(kbService.getCorpusByStatus(status));
    }

    // 标记为已索引
    @PostMapping("/mark-indexed")
    public Result<Integer> markIndexed(@RequestBody MarkIndexedReq req) {
        int count = kbService.markIndexed(req.getIds());
        return Result.success(count);
    }

    // 导入语料
    @PostMapping("/import")
    public Result<Integer> importCorpus(@RequestBody KbImportReq req) {
        int count = kbService.importCorpus(req);
        return Result.success(count);
    }

    // 获取统计信息
    @GetMapping("/stats")
    public Result<KbStatsResp> getStats() {
        return Result.success(kbService.getStats());
    }
}
