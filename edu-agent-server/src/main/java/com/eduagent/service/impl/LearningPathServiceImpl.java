package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.agent.AiClient;
import com.eduagent.entity.LearningPath;
import com.eduagent.entity.StudentProfile;
import com.eduagent.mapper.LearningPathMapper;
import com.eduagent.mapper.StudentProfileMapper;
import com.eduagent.service.LearningPathService;
import com.eduagent.vo.LearningPathVO;
import com.eduagent.vo.StageVO;
import com.eduagent.vo.TaskVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathMapper learningPathMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final AiClient aiClient;

    @Override
    public LearningPathVO getCurrentPath(Long studentId) {
        // 先查 DB 是否有缓存
        LearningPath lp = learningPathMapper.findByStudentId(studentId);
        if (lp != null && lp.getSteps() != null) {
            log.info("✅ DB 有学习路径缓存, id={}, studentId={}", lp.getId(), studentId);
            return parsePathFromJson(lp.getSteps());
        }
        // 无缓存 → 自动 AI 生成
        log.info("DB 无学习路径缓存，自动 AI 生成...");
        return generatePath(studentId);
    }

    @Override
    public LearningPathVO generatePath(Long studentId) {
        log.info("===== [学习路径生成] studentId={} =====", studentId);

        // 1. 查画像
        Map<String, Object> profileMap = new HashMap<>();
        try {
            StudentProfile sp = studentProfileMapper.findByStudentId(studentId);
            if (sp != null) {
                log.info("✅ 查到画像: course={}, topic={}, pace={}, goal={}, weaknesses={}",
                    sp.getCourse(), sp.getTopic(), sp.getPace(), sp.getLearningGoal(), sp.getWeaknesses());
                profileMap.put("course", sp.getCourse());
                profileMap.put("topic", sp.getTopic());
                profileMap.put("pace", sp.getPace());
                profileMap.put("learning_goal", sp.getLearningGoal());
                profileMap.put("weaknesses", sp.getWeaknesses());
                profileMap.put("knowledge_base", sp.getKnowledgeBase());
                profileMap.put("cognitive_style", sp.getCognitiveStyle());
                profileMap.put("resource_preference", sp.getResourcePreference());
            } else {
                log.warn("⚠️ 未找到学生画像 studentId={}", studentId);
            }
        } catch (Exception e) {
            log.error("❌ 查画像异常: {}", e.getMessage(), e);
        }

        // 2. 构造 prompt
        String profileContext = buildProfileContext(profileMap);
        String prompt = profileContext + "\n\n"
            + "请根据以上学生画像，生成一份完整的学习路径规划。\n\n"
            + "要求返回 **纯 JSON**（不要 markdown 代码块），格式如下：\n"
            + "{\n"
            + "  \"goal\": \"学习目标描述\",\n"
            + "  \"targetMastery\": \"目标掌握度如 ≥85%\",\n"
            + "  \"estimatedCompletion\": \"预计完成日期 YYYY-MM-DD\",\n"
            + "  \"totalHours\": 总学习小时数(整数),\n"
            + "  \"stages\": [\n"
            + "    {\"name\": \"今日计划\", \"tasks\": [{\"title\": \"具体任务名称\", \"duration\": 30, \"status\": 0, \"progress\": 0}]},\n"
            + "    {\"name\": \"本周路径\", \"tasks\": [{\"title\": \"具体任务名称\", \"duration\": 45, \"status\": 0, \"progress\": 0}]},\n"
            + "    {\"name\": \"考试冲刺\", \"tasks\": [{\"title\": \"具体任务名称\", \"duration\": 60, \"status\": 0, \"progress\": 0}]},\n"
            + "    {\"name\": \"实践提升\", \"tasks\": [{\"title\": \"具体任务名称\", \"duration\": 90, \"status\": 0, \"progress\": 0}]}\n"
            + "  ],\n"
            + "  \"suggestions\": \"路径调整建议文字\",\n"
            + "  \"applicationAdvice\": \"应用建议文字\",\n"
            + "  \"examAdvice\": \"阶段测评建议文字\",\n"
            + "  \"masteryRate\": 72,\n"
            + "  \"learningRate\": 18,\n"
            + "  \"unmasteredRate\": 10,\n"
            + "  \"recommendTime\": \"每天 19:00-21:00\"\n"
            + "}\n\n"
            + "重要：stages 必须有且仅有 4 个，name 严格为：今日计划、本周路径、考试冲刺、实践提升。\n"
            + "每个 stage 的 tasks 至少 2 个，title 必须是具体的知识点名称，不能写\"任务1\"\"任务2\"这种。\n"
            + "status 含义：0=待开始 1=进行中 2=已完成。\n"
            + "根据学生画像中的课程、主题、薄弱点来生成贴切的任务名称。";

        // 3. 调 AI
        Map<String, Object> aiBody = new HashMap<>();
        aiBody.put("student_id", String.valueOf(studentId));
        aiBody.put("profile", profileMap);
        aiBody.put("prompt", prompt);
        aiBody.put("mode", "generate_path");

        log.info("🚀 正在调 AI 生成学习路径...");
        String aiResult = aiClient.post("/path/generate", aiBody);
        log.info("📥 AI 返回(前500字): {}", aiResult != null && aiResult.length() > 500 ? aiResult.substring(0, 500) + "..." : aiResult);

        // 4. 解析 AI 返回的 JSON
        log.info("📥 AI 完整返回: {}", aiResult);
        String jsonStr = aiResult;
        // 清理可能的 markdown 代码块标记
        if (jsonStr != null) {
            jsonStr = jsonStr.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        }

        LearningPathVO vo;
        try {
            ObjectMapper om = new ObjectMapper();
            vo = om.readValue(jsonStr, LearningPathVO.class);
            log.info("✅ 解析 AI 返回成功: goal={}, stages={}个", vo.getGoal(),
                vo.getStages() != null ? vo.getStages().size() : 0);
            if (vo.getStages() != null) {
                for (StageVO s : vo.getStages()) {
                    log.info("  stage: {}, tasks={}个", s.getName(),
                        s.getTasks() != null ? s.getTasks().size() : 0);
                    if (s.getTasks() != null) {
                        for (TaskVO t : s.getTasks()) {
                            log.info("    task: {} ({})", t.getTitle(), t.getDuration());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ 解析 AI 返回失败: {}", e.getMessage());
            log.error("原始返回: {}", aiResult);
            // fallback: 用空数据
            vo = createFallbackPath();
        }

        // 保证 stages 不为空
        if (vo.getStages() == null || vo.getStages().isEmpty()) {
            vo.setStages(createDefaultStages());
        }
        // 计算总任务数
        if (vo.getTotalTasks() == null || vo.getTotalTasks() == 0) {
            int total = vo.getStages().stream().mapToInt(s -> s.getTasks() != null ? s.getTasks().size() : 0).sum();
            vo.setTotalTasks(total);
        }
        if (vo.getCompletedTasks() == null) vo.setCompletedTasks(0);
        if (vo.getTotalHours() == null) vo.setTotalHours(20);

        // 5. 存 DB
        try {
            ObjectMapper om = new ObjectMapper();
            String stepsJson = om.writeValueAsString(vo);
            LearningPath lp = new LearningPath();
            lp.setStudentId(studentId);
            lp.setSteps(stepsJson);
            lp.setProgress(0);
            lp.setGoal(vo.getGoal());
            lp.setStatus("active");
            lp.setCreateTime(LocalDateTime.now());
            lp.setUpdateTime(LocalDateTime.now());
            learningPathMapper.insertOrUpdate(lp);
            log.info("💾 学习路径存入DB, id={}", lp.getId());
        } catch (Exception e) {
            log.error("❌ 存DB失败: {}", e.getMessage(), e);
        }

        log.info("===== [学习路径生成] 完成 =====");
        return vo;
    }

    /** 从 JSON 解析 LearningPathVO */
    private LearningPathVO parsePathFromJson(String json) {
        try {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(json, LearningPathVO.class);
        } catch (Exception e) {
            log.error("❌ 解析DB中路径JSON失败: {}", e.getMessage());
            return createFallbackPath();
        }
    }

    /** 构建画像上下文字符串 */
    private String buildProfileContext(Map<String, Object> profile) {
        if (profile == null || profile.isEmpty()) {
            return "学生：新用户，暂无画像数据。";
        }
        StringBuilder sb = new StringBuilder("学生画像信息：\n");
        if (profile.get("course") != null) sb.append("- 课程：").append(profile.get("course")).append("\n");
        if (profile.get("topic") != null) sb.append("- 当前主题：").append(profile.get("topic")).append("\n");
        if (profile.get("pace") != null) sb.append("- 学习节奏：").append(profile.get("pace")).append("\n");
        if (profile.get("learning_goal") != null) sb.append("- 学习目标：").append(profile.get("learning_goal")).append("\n");
        if (profile.get("weaknesses") != null) sb.append("- 薄弱点：").append(profile.get("weaknesses")).append("\n");
        if (profile.get("knowledge_base") != null) sb.append("- 知识基础：").append(profile.get("knowledge_base")).append("\n");
        if (profile.get("cognitive_style") != null) sb.append("- 认知风格：").append(profile.get("cognitive_style")).append("\n");
        if (profile.get("resource_preference") != null) sb.append("- 资源偏好：").append(profile.get("resource_preference")).append("\n");
        return sb.toString();
    }

    /** fallback 空路径 */
    private LearningPathVO createFallbackPath() {
        LearningPathVO vo = new LearningPathVO();
        vo.setGoal("完成课程学习目标");
        vo.setTargetMastery("≥85%");
        vo.setEstimatedCompletion(java.time.LocalDate.now().plusDays(30).toString());
        vo.setTotalHours(20);
        vo.setTotalTasks(0);
        vo.setCompletedTasks(0);
        vo.setMasteryRate(50);
        vo.setLearningRate(20);
        vo.setUnmasteredRate(30);
        vo.setSuggestions("暂无调整建议");
        vo.setApplicationAdvice("暂无应用建议");
        vo.setExamAdvice("暂无测评建议");
        vo.setRecommendTime("每天 19:00-21:00");
        vo.setStages(createDefaultStages());
        return vo;
    }

    /** 默认 stages */
    private List<StageVO> createDefaultStages() {
        List<StageVO> stages = new ArrayList<>();
        String[] stageNames = {"今日计划", "本周路径", "考试冲刺", "实践提升"};
        for (String name : stageNames) {
            StageVO s = new StageVO();
            s.setName(name);
            s.setTasks(Arrays.asList(
                createTaskVO(name + " - 任务1", 30, 0, 0),
                createTaskVO(name + " - 任务2", 45, 0, 0)
            ));
            stages.add(s);
        }
        return stages;
    }

    private TaskVO createTaskVO(String title, int duration, int status, int progress) {
        TaskVO vo = new TaskVO();
        vo.setTitle(title);
        vo.setDuration(duration);
        vo.setStatus(status);
        vo.setProgress(progress);
        return vo;
    }
}
