package com.eduagent.controller;

import com.eduagent.common.PageResult;
import com.eduagent.common.Result;
import com.eduagent.entity.AdminLog;
import com.eduagent.service.AdminLogService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/logs")
public class AdminLogController {

    private final AdminLogService adminLogService;

    public AdminLogController(AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<AdminLog> logPage = adminLogService.getLogs(page, pageSize);
        List<Map<String, Object>> list = logPage.getContent().stream()
                .map(adminLogService::toLogVO).toList();

        return Result.ok(new PageResult<>(list, logPage.getTotalElements(), page, pageSize));
    }
}
