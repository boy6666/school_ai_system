package com.eduagent.controller;

import com.eduagent.common.PageResult;
import com.eduagent.common.Result;
import com.eduagent.dto.UpdateStatusRequest;
import com.eduagent.entity.Course;
import com.eduagent.security.AdminOperation;
import com.eduagent.service.AdminResourceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/courses")
public class AdminCourseController {

    private final AdminResourceService adminResourceService;

    public AdminCourseController(AdminResourceService adminResourceService) {
        this.adminResourceService = adminResourceService;
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<Course> coursePage = adminResourceService.getCourseList(keyword, status, page, pageSize);
        List<Map<String, Object>> list = coursePage.getContent().stream()
                .map(adminResourceService::toCourseVO).toList();

        return Result.ok(new PageResult<>(list, coursePage.getTotalElements(), page, pageSize));
    }

    @PostMapping("/{id}/status")
    @AdminOperation(value = "更新课程状态", targetType = "course")
    public Result<?> updateStatus(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest request) {
        adminResourceService.updateCourseStatus(id, request.getStatus());
        return Result.ok(Map.of("success", true));
    }
}
