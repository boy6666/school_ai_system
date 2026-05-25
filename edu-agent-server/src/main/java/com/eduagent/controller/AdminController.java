package com.eduagent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.common.PageResult;
import com.eduagent.common.Result;
import com.eduagent.entity.*;
import com.eduagent.service.*;
import com.eduagent.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AgentConfigService agentConfigService;
    private final ContentReviewService contentReviewService;
    private final ResourceService resourceService;
    private final SystemSettingService systemSettingService;

    // ===== 用户管理 =====

    @GetMapping("/users")
    public Result<PageResult<UserInfoVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        PageResult<UserInfoVO> result = adminService.listUsers(page, pageSize, keyword);
        return Result.success(result);
    }

    @PutMapping("/users/{userId}/role")
    public Result<Void> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        adminService.updateUserRole(userId, body.get("role"));
        return Result.success();
    }

    @DeleteMapping("/users/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return Result.success();
    }

    // ===== 统计 =====

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(adminService.getStats());
    }

    // ===== 智能体管理 =====

    @GetMapping("/agents")
    public Result<PageResult<AgentConfig>> listAgents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Page<AgentConfig> p = agentConfigService.listAgents(page, pageSize, keyword);
        return Result.success(new PageResult<>(p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize()));
    }

    @PostMapping("/agents")
    public Result<AgentConfig> createAgent(@RequestBody AgentConfig config) {
        agentConfigService.save(config);
        return Result.success(config);
    }

    @PutMapping("/agents/{id}")
    public Result<Void> updateAgent(@PathVariable Long id, @RequestBody AgentConfig config) {
        config.setId(id);
        agentConfigService.updateById(config);
        return Result.success();
    }

    @DeleteMapping("/agents/{id}")
    public Result<Void> deleteAgent(@PathVariable Long id) {
        agentConfigService.removeById(id);
        return Result.success();
    }

    // ===== 内容审核 =====

    @GetMapping("/conversations")
    public Result<PageResult<Conversation>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String intent) {
        Page<Conversation> p = contentReviewService.listConversations(page, pageSize, keyword, intent);
        return Result.success(new PageResult<>(p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize()));
    }

    @PutMapping("/conversations/{id}/flag")
    public Result<Void> flagConversation(@PathVariable Long id, @RequestBody Map<String, String> body) {
        contentReviewService.flagConversation(id, body.get("flag"));
        return Result.success();
    }

    @GetMapping("/conversations/stats")
    public Result<Map<String, Object>> getConversationStats() {
        return Result.success(contentReviewService.getReviewStats());
    }

    // ===== 资源管理 =====

    @GetMapping("/resources")
    public Result<PageResult<Resource>> listResources(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<Resource> p = resourceService.listResources(page, pageSize, keyword, type, status);
        return Result.success(new PageResult<>(p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize()));
    }

    @PostMapping("/resources")
    public Result<Resource> createResource(@RequestBody Resource resource) {
        resource.setStatus("published");
        resourceService.save(resource);
        return Result.success(resource);
    }

    @PutMapping("/resources/{id}")
    public Result<Void> updateResource(@PathVariable Long id, @RequestBody Resource resource) {
        resource.setId(id);
        resourceService.updateById(resource);
        return Result.success();
    }

    @DeleteMapping("/resources/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        resourceService.removeById(id);
        return Result.success();
    }

    // ===== 系统设置 =====

    @GetMapping("/settings")
    public Result<java.util.List<SystemSetting>> getSettings() {
        return Result.success(systemSettingService.listAll());
    }

    @PutMapping("/settings")
    public Result<Void> updateSetting(@RequestBody Map<String, String> body) {
        systemSettingService.updateSetting(body.get("key"), body.get("value"));
        return Result.success();
    }
}
