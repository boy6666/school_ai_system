package com.eduagent.controller;

import com.eduagent.common.PageResult;
import com.eduagent.common.Result;
import com.eduagent.dto.CreateUserRequest;
import com.eduagent.dto.UpdateUserRequest;
import com.eduagent.entity.User;
import com.eduagent.security.AdminOperation;
import com.eduagent.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<User> userPage = adminUserService.getUserList(keyword, role, status, page, pageSize);
        List<Map<String, Object>> list = userPage.getContent().stream()
                .map(adminUserService::toUserVO).toList();

        return Result.ok(new PageResult<>(list, userPage.getTotalElements(), page, pageSize));
    }

    @PostMapping
    @AdminOperation(value = "创建用户", targetType = "user")
    public Result<Map<String, Object>> create(@Valid @RequestBody CreateUserRequest request) {
        User user = adminUserService.createUser(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        return Result.ok("创建成功", data);
    }

    @PutMapping("/{id}")
    @AdminOperation(value = "更新用户", targetType = "user")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        adminUserService.updateUser(id, request);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    @AdminOperation(value = "删除用户", targetType = "user")
    public Result<?> delete(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return Result.ok("删除成功");
    }

    @PostMapping("/{id}/toggle")
    @AdminOperation(value = "切换用户状态", targetType = "user")
    public Result<?> toggleStatus(@PathVariable Long id) {
        User user = adminUserService.toggleUserStatus(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("status", user.getStatus().name());
        return Result.ok("状态更新成功", data);
    }
}
