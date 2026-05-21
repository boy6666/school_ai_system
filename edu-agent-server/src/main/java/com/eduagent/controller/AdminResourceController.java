package com.eduagent.controller;

import com.eduagent.common.PageResult;
import com.eduagent.common.Result;
import com.eduagent.dto.UpdateStatusRequest;
import com.eduagent.entity.Resource;
import com.eduagent.security.AdminOperation;
import com.eduagent.service.AdminResourceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/resources")
public class AdminResourceController {

    private final AdminResourceService adminResourceService;

    public AdminResourceController(AdminResourceService adminResourceService) {
        this.adminResourceService = adminResourceService;
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<Resource> resourcePage = adminResourceService.getResourceList(keyword, type, status, page, pageSize);
        List<Map<String, Object>> list = resourcePage.getContent().stream()
                .map(adminResourceService::toResourceVO).toList();

        return Result.ok(new PageResult<>(list, resourcePage.getTotalElements(), page, pageSize));
    }

    @PostMapping("/{id}/status")
    @AdminOperation(value = "更新资源状态", targetType = "resource")
    public Result<?> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        adminResourceService.updateResourceStatus(id, request.getStatus());
        return Result.ok(Map.of("success", true));
    }
}
