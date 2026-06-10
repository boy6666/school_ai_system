package com.eduagent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.common.PageResult;
import com.eduagent.common.Result;
import com.eduagent.agent.AiClient;
import com.eduagent.agent.AiChatResponse;
import com.eduagent.entity.*;
import com.eduagent.service.*;
import com.eduagent.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AgentConfigService agentConfigService;
    private final ContentReviewService contentReviewService;
    private final ResourceService resourceService;
    private final AiClient aiClient;
    private final SystemSettingService systemSettingService;

    // ==================== 统计 ====================
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try { stats.putAll(adminService.getStats()); } catch (Exception e) { /* DB down, return zeros */ }
        if (!stats.containsKey("totalUsers")) stats.put("totalUsers", 0);
        if (!stats.containsKey("activeUsers")) stats.put("activeUsers", 0);
        if (!stats.containsKey("totalConversations")) stats.put("totalConversations", 0);
        if (!stats.containsKey("todayConversations")) stats.put("todayConversations", 0);
        return Result.success(stats);
    }

    // ==================== 用户管理 ====================
    @GetMapping("/users")
    public Result<PageResult<UserInfoVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            PageResult<UserInfoVO> result = adminService.listUsers(page, pageSize, keyword);
            return Result.success(result);
        } catch (Exception e) {
            return Result.success(new PageResult<>(List.of(), 0, page, pageSize));
        }
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

    // ==================== 智能体管理 ====================
    @GetMapping("/agents")
    public Result<PageResult<AgentConfig>> listAgents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        try {
            Page<AgentConfig> p = agentConfigService.listAgents(page, pageSize, keyword);
            return Result.success(new PageResult<>(p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize()));
        } catch (Exception e) {
            return Result.success(new PageResult<>(List.of(), 0, page, pageSize));
        }
    }

    @PostMapping("/agents")
    public Result<AgentConfig> createAgent(@RequestBody AgentConfig config) {
        agentConfigService.save(config);
        return Result.success(config);
    }

    @PutMapping("/agents/{id}/status")
    public Result<Void> updateAgentStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AgentConfig config = agentConfigService.getById(id);
        if (config != null) {
            config.setStatus(body.get("status"));
            agentConfigService.updateById(config);
        }
        return Result.success();
    }

    // ==================== 内容审核 ====================
    @GetMapping("/conversations")
    public Result<PageResult<Conversation>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            Page<Conversation> p = contentReviewService.listConversations(page, pageSize, keyword, null);
            return Result.success(new PageResult<>(p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize()));
        } catch (Exception e) {
            return Result.success(new PageResult<>(List.of(), 0, page, pageSize));
        }
    }

    @PutMapping("/conversations/{id}/approve")
    public Result<Void> approveConversation(@PathVariable Long id) {
        contentReviewService.flagConversation(id, "approved");
        return Result.success();
    }

    @PutMapping("/conversations/{id}/reject")
    public Result<Void> rejectConversation(@PathVariable Long id, @RequestBody Map<String, String> body) {
        contentReviewService.flagConversation(id, "rejected:" + body.getOrDefault("reason", ""));
        return Result.success();
    }

    // ==================== 资源管理 ====================
    @GetMapping("/resources")
    public Result<PageResult<Resource>> listResources(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        try {
            Page<Resource> p = resourceService.listResources(page, pageSize, keyword, type, status);
            return Result.success(new PageResult<>(p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize()));
        } catch (Exception e) {
            return Result.success(new PageResult<>(List.of(), 0, page, pageSize));
        }
    }

    @PutMapping("/resources/{id}/status")
    public Result<Void> updateResourceStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Resource resource = resourceService.getById(id);
        if (resource != null) {
            resource.setStatus(body.get("status"));
            resourceService.updateById(resource);
        }
        return Result.success();
    }

    // 资源生成 — 从 AI 本地文件导入
    @PostMapping("/resources/generate")
    public Result<Map<String, Object>> generateResources(@RequestBody Map<String, String> body) {
        String studentId = body.getOrDefault("studentId", "9");
        String sessionId = "gen_" + System.currentTimeMillis();
        
        // 1. 调 AI 引擎生成资源
        AiChatResponse aiResp = aiClient.chat(studentId, sessionId, "请根据我的学习画像，为我生成完整的个性化学习资源包，包含讲解文档、思维导图、练习题、拓展阅读和代码案例");
        
        // 2. 读取 AI 生成的文件并导入数据库
        int imported = 0;
        String resourceDir = aiResp != null ? aiResp.getResourceDir() : "";
        if (resourceDir != null && !resourceDir.isEmpty()) {
            java.io.File dir = new java.io.File(resourceDir);
            if (dir.exists() && dir.isDirectory()) {
                String[][] fileTypes = {
                    {"course_doc.md", "文档"},
                    {"mindmap.mmd", "思维导图"},
                    {"quiz.json", "题库"},
                    {"extended_reading.md", "拓展阅读"},
                    {"code_practice.java", "代码案例"},
                };
                for (String[] ft : fileTypes) {
                    java.io.File f = new java.io.File(dir, ft[0]);
                    if (f.exists()) {
                        try {
                            String content = new String(java.nio.file.Files.readAllBytes(f.toPath()));
                            Resource res = new Resource();
                            res.setTitle(studentId + " - " + ft[1]);
                            res.setType(ft[1]);
                            res.setDescription("AI多智能体自动生成");
                            res.setContent(content);
                            res.setAuthor("AI多智能体系统");
                            res.setRating(4.5);
                            res.setStatus("published");
                            res.setCourseName("Java 程序设计");
                            res.setTags("[\"Java\",\"" + ft[1] + "\",\"" + studentId + "\"]");
                            res.setCreateTime(java.time.LocalDateTime.now());
                            res.setUpdateTime(java.time.LocalDateTime.now());
                            resourceService.save(res);
                            imported++;
                        } catch (Exception e) { log.warn("Failed to import {}: {}", ft[0], e.getMessage()); }
                    }
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        result.put("resourceDir", resourceDir);
        return Result.success(result);
    }



    // ==================== 系统设置 ====================
    @GetMapping("/settings")
    public Result<List<SystemSetting>> getSettings() {
        try {
            return Result.success(systemSettingService.listAll());
        } catch (Exception e) {
            return Result.success(List.of());
        }
    }

    @PutMapping("/settings")
    public Result<Void> updateSetting(@RequestBody Map<String, String> body) {
        systemSettingService.updateSetting(body.get("key"), body.get("value"));
        return Result.success();
    }
}
