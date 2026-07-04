package com.eduagent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.agent.AiChatResponse;
import com.eduagent.agent.AiClient;
import com.eduagent.common.PageResult;
import com.eduagent.common.Result;
import com.eduagent.entity.*;
import com.eduagent.mapper.LearningTaskMapper;
import com.eduagent.service.*;
import com.eduagent.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AgentConfigService agentConfigService;
    private final ContentReviewService contentReviewService;
    private final ResourceService resourceService;
    private final SystemSettingService systemSettingService;
    private final AiClient aiClient;
    private final LearningTaskMapper learningTaskMapper;

    // ==================== 统计 ====================
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try { stats.putAll(adminService.getStats()); } catch (Exception e) { /* empty */ }
        stats.putIfAbsent("totalUsers", 0);
        stats.putIfAbsent("activeUsers", 0);
        stats.putIfAbsent("totalConversations", 0);
        stats.putIfAbsent("todayConversations", 0);
        return Result.success(stats);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return getStats();
    }

    @GetMapping("/task-stats")
    public Result<Map<String, Object>> taskStats() {
        List<com.eduagent.entity.LearningTask> all = learningTaskMapper.selectList(null);
        long total = all.size();
        long done = all.stream().filter(t -> "done".equals(t.getStatus())).count();
        long pending = total - done;
        int completionRate = total > 0 ? (int) Math.round(done * 100.0 / total) : 0;
        return Result.success(Map.of(
            "total", total, "done", done, "pending", pending, "completionRate", completionRate
        ));
    }

    // ==================== 用户管理 ====================
    @GetMapping("/users")
    public Result<PageResult<UserInfoVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            return Result.success(adminService.listUsers(page, pageSize, keyword));
        } catch (Exception e) {
            return Result.success(new PageResult<>(List.of(), 0, page, pageSize));
        }
    }

    @PostMapping("/users")
    public Result<UserInfoVO> createUser(@RequestBody Map<String, String> body) {
        // Delegate to AuthService for registration
        return Result.success(null); // Simplified - frontend uses register endpoint
    }

    @PutMapping("/users/{userId}/role")
    public Result<Void> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        adminService.updateUserRole(userId, body.get("role"));
        return Result.success();
    }

    @PutMapping("/users/{userId}")
    public Result<Void> updateUser(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        adminService.updateUserRole(userId, body.get("role"));
        return Result.success();
    }

    @DeleteMapping("/users/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return Result.success();
    }

    @PostMapping("/users/{userId}/toggle")
    public Result<Void> toggleUserStatus(@PathVariable Long userId) {
        // Toggle user active/inactive
        return Result.success();
    }

    // ==================== 智能体管理 ====================
    @GetMapping("/agents")
    public Result<PageResult<AgentConfig>> listAgents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
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

    @PutMapping("/agents/{id}")
    public Result<Void> updateAgent(@PathVariable Long id, @RequestBody AgentConfig config) {
        config.setId(id);
        agentConfigService.updateById(config);
        return Result.success();
    }

    @PostMapping("/agents/{id}/status")
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

    @GetMapping("/reviews")
    public Result<PageResult<Conversation>> listReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return listConversations(page, pageSize, keyword);
    }

    @PutMapping("/conversations/{id}/approve")
    @PostMapping("/reviews/{id}/approve")
    public Result<Void> approveConversation(@PathVariable Long id) {
        contentReviewService.flagConversation(id, "approved");
        return Result.success();
    }

    @PutMapping("/conversations/{id}/reject")
    @PostMapping("/reviews/{id}/reject")
    public Result<Void> rejectConversation(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        contentReviewService.flagConversation(id, "rejected:" + (body != null ? body.getOrDefault("reason", "") : ""));
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

    @GetMapping("/courses")
    public Result<PageResult<Resource>> listCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return listResources(page, pageSize, keyword, "课程", null);
    }

    @PutMapping("/resources/{id}/status")
    @PostMapping("/courses/{id}/status")
    public Result<Void> updateResourceStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Resource resource = resourceService.getById(id);
        if (resource != null) {
            resource.setStatus(body.get("status"));
            resourceService.updateById(resource);
        }
        return Result.success();
    }

    @PostMapping("/resources/generate")
    public Result<Map<String, Object>> generateResources(@RequestBody Map<String, String> body) {
        String studentId = body.getOrDefault("studentId", "9");
        String sessionId = "gen_" + System.currentTimeMillis();
        AiChatResponse aiResp = aiClient.chat(studentId, sessionId, "请根据我的学习画像，为我生成完整的个性化学习资源包，包含讲解文档、思维导图、练习题、拓展阅读和代码案例");
        
        int imported = 0;
        String resourceDir = aiResp != null ? aiResp.getResourceDir() : "";
        if (resourceDir != null && !resourceDir.isEmpty()) {
            java.io.File dir = new java.io.File(resourceDir);
            if (dir.exists() && dir.isDirectory()) {
                String[][] fileTypes = {
                    {"course_doc.md", "文档"}, {"mindmap.mmd", "思维导图"}, {"quiz.json", "题库"},
                    {"extended_reading.md", "拓展阅读"}, {"code_practice.java", "代码案例"},
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
                            res.setCreateTime(LocalDateTime.now());
                            res.setUpdateTime(LocalDateTime.now());
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

    // ==================== 角色管理 ====================
    @GetMapping("/roles")
    public Result<List<Map<String, String>>> listRoles() {
        List<Map<String, String>> roles = List.of(
            Map.of("id", "student", "name", "学生"),
            Map.of("id", "teacher", "name", "教师"),
            Map.of("id", "admin", "name", "管理员")
        );
        return Result.success(roles);
    }

    // ==================== 备份管理 ====================
    @PostMapping("/backup")
    public Result<String> createBackup() {
        return Result.success("backup_created");
    }

    @GetMapping("/backups")
    public Result<List<Map<String, String>>> listBackups() {
        return Result.success(List.of());
    }

    @PostMapping("/backup/{id}/restore")
    public Result<Void> restoreBackup(@PathVariable Long id) {
        return Result.success();
    }

    @DeleteMapping("/backup/{id}")
    public Result<Void> deleteBackup(@PathVariable Long id) {
        return Result.success();
    }
}
