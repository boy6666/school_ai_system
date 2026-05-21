package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.security.AdminOperation;
import com.eduagent.service.AdminRoleService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/roles")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    public AdminRoleController(AdminRoleService adminRoleService) {
        this.adminRoleService = adminRoleService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(adminRoleService.getAllRoles());
    }

    @PostMapping
    @AdminOperation(value = "创建角色", targetType = "role")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> params) {
        return Result.ok("创建成功", adminRoleService.createRole(params));
    }

    @PutMapping("/{id}")
    @AdminOperation(value = "更新角色", targetType = "role")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        adminRoleService.updateRole(id, params);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    @AdminOperation(value = "删除角色", targetType = "role")
    public Result<?> delete(@PathVariable Long id) {
        adminRoleService.deleteRole(id);
        return Result.ok("删除成功");
    }
}
