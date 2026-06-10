package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.agent.AiClient;
import com.eduagent.entity.StudentProfile;
import com.eduagent.mapper.StudentProfileMapper;
import com.eduagent.mapper.QuizAnswerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final StudentProfileMapper studentProfileMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final AiClient aiClient;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        Map<String, Object> item = new HashMap<>();
        item.put("message", "profile stub - pending");
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(item);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        log.info("===== [画像查询] studentId={} =====", id);
        Map<String, Object> map = new HashMap<>();
        try {
            StudentProfile sp = studentProfileMapper.findByStudentId(id);
            if (sp != null) {
                log.info("✅ 查到画像: id={}, course={}, topic={}, pace={}", sp.getId(), sp.getCourse(), sp.getTopic(), sp.getPace());
                map.put("id", sp.getId());
                map.put("student_id", sp.getStudentId());
                map.put("course", sp.getCourse());
                map.put("topic", sp.getTopic());
                map.put("learning_goal", sp.getLearningGoal());
                map.put("knowledge_base", sp.getKnowledgeBase());
                map.put("cognitive_style", sp.getCognitiveStyle());
                map.put("pace", sp.getPace());
                map.put("weaknesses", sp.getWeaknesses() != null ? Arrays.asList(sp.getWeaknesses().replaceAll("[\\[\\]\"]", "").split("\\s*,\\s*")) : null);
                map.put("mistake_patterns", sp.getMistakePatterns() != null ? Arrays.asList(sp.getMistakePatterns().replaceAll("[\\[\\]\"]", "").split("\\s*,\\s*")) : null);
                map.put("resource_preference", sp.getResourcePreference() != null ? Arrays.asList(sp.getResourcePreference().replaceAll("[\\[\\]\"]", "").split("\\s*,\\s*")) : null);
                map.put("last_score", sp.getLastScore());
                map.put("profile_suggestions", sp.getProfileSuggestions() != null ? Arrays.asList(sp.getProfileSuggestions().split("\\n")) : null);
                map.put("overall_type", sp.getOverallType());
                map.put("last_suggestion", sp.getLastSuggestion());
                map.put("last_updated", sp.getUpdateTime() != null ? sp.getUpdateTime().toString().replace("T", " ") : null);
                // 返回六维画像分数
                if (sp.getProfileData() != null) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                        map.put("profile_data", om.readValue(sp.getProfileData(), Map.class));
                    } catch (Exception ignored) {}
                }
                // 答题统计
                try {
                    long quizCount = quizAnswerMapper.selectCount(
                        new LambdaQueryWrapper<com.eduagent.entity.QuizAnswer>()
                            .eq(com.eduagent.entity.QuizAnswer::getStudentId, id)
                    );
                    map.put("quizCount", quizCount);
                } catch (Exception ignored) {}
                map.put("exists", true);
            } else {
                log.warn("⚠️ 未找到画像 studentId={}", id);
                map.put("exists", false);
                map.put("message", "画像未找到");
            }
        } catch (Exception e) {
            log.error("❌ 查画像异常: {}", e.getMessage(), e);
            map.put("exists", false);
            map.put("error", e.getMessage());
        }
        return Result.success(map);
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "created");
        map.put("message", "profile stub - pending");
        return Result.success(map);
    }

    /**
     * 保存/更新画像 — 由引导流程/日常对话调用，支持全量画像字段
     * POST /profile/save
     */
    @PostMapping("/save")
    public Result<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        Long userId;
        try {
            userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            return Result.error("未登录");
        }
        log.info("===== [画像保存] userId={}, body={}", userId, body);

        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            StudentProfile sp = studentProfileMapper.findByStudentId(userId);
            if (sp == null) {
                sp = new StudentProfile();
                sp.setStudentId(userId);
                sp.setCreateTime(LocalDateTime.now());
            }
            // 更新画像字段（基础字段）
            if (body.containsKey("pace")) sp.setPace((String) body.get("pace"));
            if (body.containsKey("learning_goal")) sp.setLearningGoal((String) body.get("learning_goal"));
            if (body.containsKey("topic")) sp.setTopic((String) body.get("topic"));
            if (body.containsKey("course")) sp.setCourse((String) body.get("course"));
            if (body.containsKey("knowledge_base")) sp.setKnowledgeBase((String) body.get("knowledge_base"));
            if (body.containsKey("cognitive_style")) sp.setCognitiveStyle((String) body.get("cognitive_style"));
            if (body.containsKey("overall_type")) sp.setOverallType((String) body.get("overall_type"));

            // 数组/对象字段 — 转为 JSON 字符串存储
            Object weaknesses = body.get("weaknesses");
            if (weaknesses != null) sp.setWeaknesses(om.writeValueAsString(weaknesses));
            Object resourcePref = body.get("resource_preference");
            if (resourcePref != null) sp.setResourcePreference(om.writeValueAsString(resourcePref));
            Object mistakePatterns = body.get("mistake_patterns");
            if (mistakePatterns != null) sp.setMistakePatterns(om.writeValueAsString(mistakePatterns));

            // 六维画像数据
            Object dimensions = body.get("dimensions");
            if (dimensions != null) {
                sp.setProfileData(om.writeValueAsString(dimensions));
                // 从六维中提取 last_score
                if (dimensions instanceof Map) {
                    Map<?, ?> dimMap = (Map<?, ?>) dimensions;
                    Object overall = dimMap.get("overall_level");
                    if (overall instanceof Map) {
                        Object score = ((Map<?, ?>) overall).get("score");
                        if (score instanceof Number) sp.setLastScore(((Number) score).intValue());
                    }
                }
            }

            sp.setUpdateTime(LocalDateTime.now());

            if (sp.getId() != null) {
                studentProfileMapper.updateById(sp);
                log.info("✅ 画像更新成功, id={}", sp.getId());
            } else {
                studentProfileMapper.insert(sp);
                log.info("✅ 画像创建成功, id={}", sp.getId());
            }

            return Result.success(Map.of("id", sp.getId(), "status", "saved"));
        } catch (Exception e) {
            log.error("❌ 画像保存失败: {}", e.getMessage(), e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * AI 生成学习建议 → 存入 DB → 返回
     */
    @PostMapping("/generate-suggestions")
    public Result<Map<String, Object>> generateSuggestions(@RequestBody Map<String, Object> body) {
        Object userIdObj = body.get("userId");
        if (userIdObj == null) return Result.error("userId 不能为空");
        Long userId = userIdObj instanceof Number
            ? ((Number) userIdObj).longValue()
            : Long.parseLong(userIdObj.toString());

        log.info("===== [画像建议生成] studentId={} =====", userId);

        // 查画像
        StudentProfile sp = null;
        try {
            sp = studentProfileMapper.findByStudentId(userId);
        } catch (Exception e) {
            log.error("❌ 查画像异常: {}", e.getMessage());
        }

        // 构建 prompt
        StringBuilder profileInfo = new StringBuilder();
        if (sp != null) {
            profileInfo.append("学生画像信息：\n");
            if (sp.getCourse() != null) profileInfo.append("- 课程：").append(sp.getCourse()).append("\n");
            if (sp.getTopic() != null) profileInfo.append("- 当前主题：").append(sp.getTopic()).append("\n");
            if (sp.getPace() != null) profileInfo.append("- 学习节奏：").append(sp.getPace()).append("\n");
            if (sp.getLearningGoal() != null) profileInfo.append("- 学习目标：").append(sp.getLearningGoal()).append("\n");
            if (sp.getKnowledgeBase() != null) profileInfo.append("- 知识基础：").append(sp.getKnowledgeBase()).append("\n");
            if (sp.getWeaknesses() != null) profileInfo.append("- 薄弱点：").append(sp.getWeaknesses()).append("\n");
            if (sp.getCognitiveStyle() != null) profileInfo.append("- 认知风格：").append(sp.getCognitiveStyle()).append("\n");
            if (sp.getOverallType() != null) profileInfo.append("- 综合类型：").append(sp.getOverallType()).append("\n");
        } else {
            profileInfo.append("新用户，暂无画像数据。\n");
        }

        String prompt = profileInfo.toString()
            + "\n请根据以上学生画像，生成4条个性化的学习建议。\n"
            + "要求返回 JSON 格式：{\"suggestions\": [\"建议1\", \"建议2\", \"建议3\", \"建议4\"]}\n"
            + "建议要具体、可执行，针对学生的薄弱点和学习目标。纯 JSON 不要 markdown。";

        // 调 AI
        Map<String, Object> aiBody = new HashMap<>();
        aiBody.put("student_id", String.valueOf(userId));
        aiBody.put("prompt", prompt);
        aiBody.put("resourceType", "suggestion");
        aiBody.put("mode", "generate_suggestions");

        log.info("🚀 调 AI 生成建议...");
        String aiResult = aiClient.post("/resource/generate", aiBody);

        List<String> suggestions = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> aiMap = om.readValue(aiResult, Map.class);
            if (aiMap.containsKey("content")) {
                String content = (String) aiMap.get("content");
                // 尝试从 content 中提取 JSON
                String cleanJson = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                Map<String, Object> suggestionMap = om.readValue(cleanJson, Map.class);
                Object sugObj = suggestionMap.get("suggestions");
                if (sugObj instanceof List) {
                    for (Object s : (List<?>) sugObj) {
                        suggestions.add(s.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ 解析建议失败: {}", e.getMessage());
        }

        // fallback
        if (suggestions.isEmpty()) {
            suggestions.add("从基础概念开始，系统学习核心知识点");
            suggestions.add("每天坚持30分钟代码实践");
            suggestions.add("使用思维导图梳理知识体系");
            suggestions.add("多做练习，巩固薄弱环节");
        }

        // 存 DB
        if (sp != null) {
            String suggestionsStr = String.join("\n", suggestions);
            sp.setProfileSuggestions(suggestionsStr);
            sp.setUpdateTime(LocalDateTime.now());
            studentProfileMapper.updateById(sp);
            log.info("💾 建议已存入 DB, profileId={}", sp.getId());
        }

        log.info("===== [画像建议生成] 完成, {}条建议 =====", suggestions.size());
        return Result.success(Map.of("suggestions", suggestions));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("status", "updated");
        return Result.success(map);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return Result.success();
    }
}
