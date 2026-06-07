package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.agent.AiClient;
import com.eduagent.entity.StudentProfile;
import com.eduagent.entity.LearningPath;
import com.eduagent.entity.LearningTask;
import com.eduagent.entity.Report;
import com.eduagent.mapper.LearningPathMapper;
import com.eduagent.mapper.LearningTaskMapper;
import com.eduagent.mapper.StudentProfileMapper;
import com.eduagent.mapper.StudyLogMapper;
import com.eduagent.mapper.ReportMapper;
import com.eduagent.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StudyLogMapper studyLogMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final LearningTaskMapper learningTaskMapper;
    private final LearningPathMapper learningPathMapper;
    private final ReportMapper reportMapper;
    private final AiClient aiClient;
    private final ResourceService resourceService;

    /**
     * AI 生成学习总结（基于画像 + 路径完成情况 + 学习时长） → 存 DB → 返回
     */
    /**
     * 获取最新 AI 学习总结（从 DB 读）
     */
    @GetMapping("/ai-summary")
    public Result<Map<String, Object>> getAiSummary() {
        Long userId = getUserIdSafe();
        if (userId == null) return Result.success(new HashMap<>());
        Report report = reportMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Report>()
                .eq(Report::getStudentId, userId)
                .eq(Report::getTitle, "AI学习总结 - " + LocalDate.now().toString())
                .orderByDesc(Report::getCreateTime)
                .last("LIMIT 1")
        );
        if (report != null && report.getContent() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> result = om.readValue(report.getContent(), Map.class);
                return Result.success(result);
            } catch (Exception e) {
                log.error("❌ 解析已有总结失败: {}", e.getMessage());
            }
        }
        return Result.success(new HashMap<>());
    }

    /**
     * AI 生成学习回顾（基于画像 + 完成的任务 + 学习时长 + 阅读材料）
     */
    @PostMapping("/learning-review")
    public Result<Map<String, Object>> learningReview() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("===== [AI学习回顾] studentId={} =====", userId);

        // 收集数据
        StringBuilder dataBuilder = new StringBuilder();

        // 画像
        StudentProfile sp = studentProfileMapper.findByStudentId(userId);
        if (sp != null) {
            dataBuilder.append("【学生画像】\n");
            if (sp.getCourse() != null) dataBuilder.append("课程：").append(sp.getCourse()).append("\n");
            if (sp.getTopic() != null) dataBuilder.append("当前主题：").append(sp.getTopic()).append("\n");
            if (sp.getPace() != null) dataBuilder.append("学习节奏：").append(sp.getPace()).append("\n");
            if (sp.getLearningGoal() != null) dataBuilder.append("学习目标：").append(sp.getLearningGoal()).append("\n");
            if (sp.getKnowledgeBase() != null) dataBuilder.append("知识基础：").append(sp.getKnowledgeBase()).append("\n");
            if (sp.getWeaknesses() != null) dataBuilder.append("薄弱点：").append(sp.getWeaknesses()).append("\n");
        }

        // 学习时长（按模块）
        dataBuilder.append("\n【学习时长统计】\n");
        Integer totalSec = studyLogMapper.totalDuration(userId);
        int totalHours = totalSec != null ? totalSec / 3600 : 0;
        int totalMinutes = totalSec != null ? (totalSec % 3600) / 60 : 0;
        dataBuilder.append("总学习时长：").append(totalHours).append("小时").append(totalMinutes).append("分钟\n");
        List<Map<String, Object>> moduleSummary = studyLogMapper.moduleSummary(userId);
        if (moduleSummary != null && !moduleSummary.isEmpty()) {
            for (Map<String, Object> m : moduleSummary) {
                long min = m.get("total") != null ? Long.parseLong(m.get("total").toString()) / 60 : 0;
                dataBuilder.append("  - ").append(m.get("module")).append("：").append(min).append("分钟\n");
            }
        }

        // 任务完成情况
        List<LearningTask> allTasks = learningTaskMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningTask>()
                .eq(LearningTask::getUserId, userId)
        );
        long doneCount = allTasks.stream().filter(t -> "completed".equals(t.getStatus()) || "done".equals(t.getStatus())).count();
        dataBuilder.append("\n【任务完成】\n");
        dataBuilder.append("总任务数：").append(allTasks.size()).append("，已完成：").append(doneCount).append("\n");
        for (LearningTask t : allTasks) {
            if ("completed".equals(t.getStatus()) || "done".equals(t.getStatus())) {
                dataBuilder.append("  ✅ ").append(t.getTitle()).append("\n");
            }
        }

        // 已生成的资源
        List<com.eduagent.entity.Resource> resources = resourceService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.eduagent.entity.Resource>()
                .eq(com.eduagent.entity.Resource::getStudentId, userId)
                .eq(com.eduagent.entity.Resource::getStatus, "published")
        );
        if (resources != null && !resources.isEmpty()) {
            dataBuilder.append("\n【已学习材料】\n");
            for (com.eduagent.entity.Resource r : resources) {
                dataBuilder.append("  - ").append(r.getTitle()).append("（").append(r.getType()).append("，").append(r.getDifficulty()).append("）\n");
            }
        }

        // 构建 prompt 调 AI
        String prompt = dataBuilder.toString()
            + "\n请根据以上学习数据，生成一份学习回顾。\n"
            + "要求返回 JSON 格式（纯JSON，不要markdown）：\n"
            + "{\n"
            + "  \"summary\": \"总体回顾，200字以内，描述学习了哪些内容\",\n"
            + "  \"completedContent\": \"已完成的学习内容总结，150字以内\",\n"
            + "  \"timeAnalysis\": \"时间分配分析，100字以内\",\n"
            + "  \"nextStep\": \"建议下一步学习方向，100字以内\"\n"
            + "}";

        Map<String, Object> aiBody = new HashMap<>();
        aiBody.put("student_id", String.valueOf(userId));
        aiBody.put("prompt", prompt);
        aiBody.put("resourceType", "review");
        aiBody.put("mode", "generate_review");

        log.info("🚀 调 AI 生成学习回顾...");
        String aiResult = aiClient.post("/resource/generate", aiBody);
        log.info("📥 AI 返回(前300字): {}", aiResult != null && aiResult.length() > 300 ? aiResult.substring(0, 300) : aiResult);

        // 解析
        Map<String, Object> result = new HashMap<>();
        try {
            String jsonStr = aiResult;
            if (jsonStr != null) {
                jsonStr = jsonStr.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> aiMap = om.readValue(jsonStr, Map.class);
            if (aiMap.containsKey("content")) {
                String content = (String) aiMap.get("content");
                aiMap = om.readValue(content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim(), Map.class);
            }
            result.putAll(aiMap);
        } catch (Exception e) {
            log.error("❌ 解析AI返回失败: {}", e.getMessage());
            result.put("summary", "暂无学习回顾数据");
            result.put("completedContent", "");
            result.put("timeAnalysis", "");
            result.put("nextStep", "继续当前学习计划");
        }

        // 存 DB
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Report report = new Report();
            report.setStudentId(userId);
            report.setTitle("AI学习回顾 - " + LocalDate.now().toString());
            report.setContent(om.writeValueAsString(result));
            report.setPeriodStart(LocalDate.now().minusDays(7));
            report.setPeriodEnd(LocalDate.now());
            report.setCreateTime(LocalDateTime.now());
            reportMapper.insert(report);
            log.info("💾 学习回顾已存入 report 表");
        } catch (Exception e) {
            log.error("❌ 存DB失败: {}", e.getMessage());
        }

        return Result.success(result);
    }

    /** 获取最新 AI 学习回顾 */
    @GetMapping("/learning-review")
    public Result<Map<String, Object>> getLearningReview() {
        Long userId = getUserIdSafe();
        if (userId == null) return Result.success(new HashMap<>());
        Report report = reportMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Report>()
                .eq(Report::getStudentId, userId)
                .like(Report::getTitle, "AI学习回顾")
                .orderByDesc(Report::getCreateTime)
                .last("LIMIT 1")
        );
        if (report != null && report.getContent() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                return Result.success(om.readValue(report.getContent(), Map.class));
            } catch (Exception e) {
                log.error("❌ 解析已有回顾失败: {}", e.getMessage());
            }
        }
        return Result.success(new HashMap<>());
    }

    private Long getUserIdSafe() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof Long) return (Long) principal;
            if (principal instanceof String) return Long.parseLong((String) principal);
            return null;
        } catch (Exception e) { return null; }
    }

    @PostMapping("/ai-summary")
    public Result<Map<String, Object>> aiSummary() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("===== [AI学习总结] studentId={} =====", userId);

        // 1. 收集所有数据
        StringBuilder dataBuilder = new StringBuilder();

        // 画像
        StudentProfile sp = studentProfileMapper.findByStudentId(userId);
        if (sp != null) {
            dataBuilder.append("【学生画像】\n");
            if (sp.getCourse() != null) dataBuilder.append("课程：").append(sp.getCourse()).append("\n");
            if (sp.getTopic() != null) dataBuilder.append("当前主题：").append(sp.getTopic()).append("\n");
            if (sp.getPace() != null) dataBuilder.append("学习节奏：").append(sp.getPace()).append("\n");
            if (sp.getLearningGoal() != null) dataBuilder.append("学习目标：").append(sp.getLearningGoal()).append("\n");
            if (sp.getKnowledgeBase() != null) dataBuilder.append("知识基础：").append(sp.getKnowledgeBase()).append("\n");
            if (sp.getWeaknesses() != null) dataBuilder.append("薄弱点：").append(sp.getWeaknesses()).append("\n");
            if (sp.getCognitiveStyle() != null) dataBuilder.append("认知风格：").append(sp.getCognitiveStyle()).append("\n");
            if (sp.getOverallType() != null) dataBuilder.append("综合类型：").append(sp.getOverallType()).append("\n");
            if (sp.getLastScore() != null) dataBuilder.append("最近评分：").append(sp.getLastScore()).append("\n");
        }

        // 学习路径
        LearningPath lp = learningPathMapper.findByStudentId(userId);
        if (lp != null) {
            dataBuilder.append("\n【学习路径】\n");
            dataBuilder.append("目标：").append(lp.getGoal() != null ? lp.getGoal() : "未设置").append("\n");
            dataBuilder.append("进度：").append(lp.getProgress() != null ? lp.getProgress() + "%" : "0%").append("\n");
            dataBuilder.append("建议：").append(lp.getSuggestions() != null ? lp.getSuggestions() : "无").append("\n");
        }

        // 学习时长
        Integer totalSec = studyLogMapper.totalDuration(userId);
        int totalHours = totalSec != null ? totalSec / 3600 : 0;
        int totalMinutes = totalSec != null ? (totalSec % 3600) / 60 : 0;
        dataBuilder.append("\n【学习时长】\n");
        dataBuilder.append("总学习时长：").append(totalHours).append("小时").append(totalMinutes).append("分钟\n");

        List<Map<String, Object>> moduleSummary = studyLogMapper.moduleSummary(userId);
        if (moduleSummary != null && !moduleSummary.isEmpty()) {
            dataBuilder.append("分模块时长：\n");
            for (Map<String, Object> m : moduleSummary) {
                dataBuilder.append("  - ").append(m.get("module")).append("：").append(m.get("total") != null ? m.get("total") + "秒" : "0").append("\n");
            }
        }

        // 任务完成情况
        List<LearningTask> allTasks = learningTaskMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningTask>()
                .eq(LearningTask::getUserId, userId)
        );
        long doneCount = allTasks.stream().filter(t -> "completed".equals(t.getStatus()) || "done".equals(t.getStatus())).count();
        dataBuilder.append("\n【任务完成】\n");
        dataBuilder.append("总任务数：").append(allTasks.size()).append("，已完成：").append(doneCount).append("\n");

        // 2. 构建 prompt
        String prompt = dataBuilder.toString()
            + "\n请根据以上数据，生成一份学习总结。\n"
            + "要求返回 JSON 格式（纯JSON，不要markdown）：\n"
            + "{\n"
            + "  \"summary\": \"总体学习总结，200字以内\",\n"
            + "  \"strengths\": \"做得好的方面，100字以内\",\n"
            + "  \"weaknessAnalysis\": \"需要改进的方面，100字以内\",\n"
            + "  \"suggestion\": \"下一步学习建议，100字以内\",\n"
            + "  \"score\": 综合评分(0-100整数),\n"
            + "  \"focusNext\": \"建议重点学习内容\"\n"
            + "}";

        // 3. 调 AI
        Map<String, Object> aiBody = new HashMap<>();
        aiBody.put("student_id", String.valueOf(userId));
        aiBody.put("prompt", prompt);
        aiBody.put("resourceType", "summary");
        aiBody.put("mode", "generate_summary");

        log.info("🚀 调 AI 生成学习总结...");
        String aiResult = aiClient.post("/resource/generate", aiBody);
        log.info("📥 AI 返回(前300字): {}", aiResult != null && aiResult.length() > 300 ? aiResult.substring(0, 300) : aiResult);

        // 4. 解析
        Map<String, Object> result = new HashMap<>();
        try {
            String jsonStr = aiResult;
            if (jsonStr != null) {
                jsonStr = jsonStr.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> aiMap = om.readValue(jsonStr, Map.class);
            // 如果 AI 返回了 content 字段（/resource/generate 包装），提取它
            if (aiMap.containsKey("content")) {
                String content = (String) aiMap.get("content");
                aiMap = om.readValue(content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim(), Map.class);
            }
            result.putAll(aiMap);
            log.info("✅ 解析成功: score={}, summary={}", aiMap.get("score"), 
                aiMap.get("summary") != null ? ((String)aiMap.get("summary")).substring(0, Math.min(50, ((String)aiMap.get("summary")).length())) : "");
        } catch (Exception e) {
            log.error("❌ 解析 AI 返回失败: {}", e.getMessage());
            result.put("summary", "数据分析中，请稍后再试");
            result.put("score", 0);
        }

        // 5. 存 DB（report 表）
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Report report = new Report();
            report.setStudentId(userId);
            report.setTitle("AI学习总结 - " + LocalDate.now().toString());
            report.setContent(om.writeValueAsString(result));
            report.setPeriodStart(LocalDate.now().minusDays(7));
            report.setPeriodEnd(LocalDate.now());
            report.setCreateTime(LocalDateTime.now());
            reportMapper.insert(report);
            log.info("💾 学习总结已存入 report 表, id={}", report.getId());
        } catch (Exception e) {
            log.error("❌ 存DB失败: {}", e.getMessage());
        }

        log.info("===== [AI学习总结] 完成 =====");
        return Result.success(result);
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Map<String, Object>> today = studyLogMapper.todaySummary(userId);
        Integer total = studyLogMapper.totalDuration(userId);
        Map<String, Object> r = new HashMap<>();
        r.put("today", today);
        r.put("totalSec", total != null ? total : 0);
        return Result.success(r);
    }

    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> tasks() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<LearningTask> list = learningTaskMapper.selectPendingByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LearningTask t : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("status", t.getStatus());
            m.put("priority", t.getPriority());
            result.add(m);
        }
        return Result.success(result);
    }

    @GetMapping("/path")
    public Result<Map<String, Object>> path() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LearningPath lp = learningPathMapper.findByStudentId(userId);
        Map<String, Object> r = new HashMap<>();
        if (lp != null) {
            r.put("goal", lp.getGoal());
            r.put("pace", lp.getPace());
            r.put("progress", lp.getProgress());
            r.put("suggestions", lp.getSuggestions());
        }
        return Result.success(r);
    }

    @GetMapping("/report")
    public Result<Map<String, Object>> report() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> r = new HashMap<>();

        // 总时长
        Integer total = studyLogMapper.totalDuration(userId);
        r.put("totalSec", total != null ? total : 0);

        // 分模块时长
        List<Map<String, Object>> modules = studyLogMapper.moduleSummary(userId);
        r.put("modules", modules);

        // 近7天趋势
        List<Map<String, Object>> trend = studyLogMapper.dailyTrend(userId);
        r.put("trend", trend);

        // 画像数据（六维）
        StudentProfile sp = studentProfileMapper.findByStudentId(userId);
        if (sp != null) {
            r.put("score", sp.getLastScore() != null ? sp.getLastScore() : 0);
            r.put("learning_goal", sp.getLearningGoal());
            r.put("cognitive_style", sp.getCognitiveStyle());
            r.put("pace", sp.getPace());
            r.put("topic", sp.getTopic());
            r.put("course", sp.getCourse());
            r.put("last_suggestion", sp.getLastSuggestion());
            r.put("profile_suggestions", sp.getProfileSuggestions());
            if (sp.getProfileData() != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    r.put("profile_data", om.readValue(sp.getProfileData(), Map.class));
                } catch (Exception ignored) {}
            }
            if (sp.getWeaknesses() != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    r.put("weaknesses", om.readValue(sp.getWeaknesses(), List.class));
                } catch (Exception ignored) {}
            }
        }

        // 学习路径数据
        LearningPath lp = learningPathMapper.findByStudentId(userId);
        if (lp != null) {
            r.put("goal", lp.getGoal());
            r.put("progress", lp.getProgress());
        }

        return Result.success(r);
    }

    @GetMapping("/evaluation")
    public Result<Map<String, Object>> evaluation() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        StudentProfile sp = studentProfileMapper.findByStudentId(userId);
        Map<String, Object> result = new HashMap<>();
        if (sp != null) {
            result.put("score", sp.getLastScore() != null ? sp.getLastScore() : 0);
            result.put("learning_goal", sp.getLearningGoal());
            result.put("cognitive_style", sp.getCognitiveStyle());
            result.put("pace", sp.getPace());
            result.put("course", sp.getCourse());
            result.put("topic", sp.getTopic());
            // Parse weaknesses JSON if available
            if (sp.getWeaknesses() != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    result.put("weaknesses", om.readValue(sp.getWeaknesses(), List.class));
                } catch (Exception e) {
                    result.put("weaknesses", sp.getWeaknesses());
                }
            }
        }
        return Result.success(result);
    }
}
