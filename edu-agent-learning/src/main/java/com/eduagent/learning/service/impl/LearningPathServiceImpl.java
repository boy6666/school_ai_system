package com.eduagent.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.learning.dto.ai.AiPathRequest;
import com.eduagent.learning.entity.LearningPath;
import com.eduagent.learning.entity.LearningPathHistory;
import com.eduagent.learning.entity.LearningTask;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.LearningPathHistoryMapper;
import com.eduagent.learning.mapper.LearningPathMapper;
import com.eduagent.learning.mapper.LearningTaskMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.eduagent.learning.mq.StudyProgressPublisher;
import com.eduagent.learning.service.LearningPathService;
import com.eduagent.learning.vo.LearningPathVO;
import com.eduagent.learning.vo.PathHistoryVO;
import com.eduagent.learning.vo.StageVO;
import com.eduagent.learning.vo.TaskVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习路径。字段分工（决议 C9）：AI 负责 goal/targetMastery/totalHours/masteryRate/
 * stages/suggestions/applicationAdvice/examAdvice/recommendTime；
 * 本服务自算补全 totalTasks/completedTasks/learningRate/unmasteredRate 与 tasks[].id。
 * 修正单体的两个空白：learning_path_history 补写入；重新生成时清理旧任务防重复。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    /** stage 表键 ↔ 前端展示名 */
    private static final Map<String, String> STAGE_NAME = Map.of(
            "today", "今日计划", "week", "本周路径", "exam", "考试冲刺", "practice", "实践提升");
    private static final Map<String, String> NAME_STAGE = Map.of(
            "今日计划", "today", "本周路径", "week", "考试冲刺", "exam", "实践提升", "practice");

    private static final Map<String, Integer> STATUS_CODE = Map.of("todo", 0, "doing", 1, "done", 2);
    private static final Map<Integer, String> CODE_STATUS = Map.of(0, "todo", 1, "doing", 2, "done");

    private final LearningPathMapper pathMapper;
    private final LearningTaskMapper taskMapper;
    private final LearningPathHistoryMapper historyMapper;
    private final StudentProfileMapper profileMapper;
    private final AiServiceClient aiServiceClient;
    private final AiResultParser aiResultParser;
    private final StudyProgressPublisher progressPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public LearningPathVO getCurrentPath(Long studentId) {
        List<LearningTask> tasks = taskMapper.selectByUserId(studentId);
        LearningPath lp = pathMapper.findActiveByStudentId(studentId);
        if ((tasks == null || tasks.isEmpty()) && lp == null) {
            log.info("[Path] 无路径数据, 自动生成 studentId={}", studentId);
            return generatePath(studentId);
        }

        LearningPathVO base = parseStored(lp);
        if (base == null) {
            base = new LearningPathVO();
            base.setGoal("完成任务清单");
            base.setTargetMastery("≥85%");
        }
        // 以 learning_tasks 为任务状态权威源重建 stages
        base.setStages(buildStagesFromTasks(tasks));
        fillAggregates(base);
        return base;
    }

    @Override
    @Transactional
    public LearningPathVO generatePath(Long studentId) {
        log.info("[Path] 生成学习路径 studentId={}", studentId);

        Map<String, Object> profileMap = buildProfileMap(studentId);
        LearningPathVO vo = requestAiPath(studentId, profileMap);

        // 补全聚合字段 + 持久化
        fillAggregates(vo);
        int completed = countDone(vo);
        int total = countTotal(vo);
        int progress = total > 0 ? (int) Math.round(completed * 100.0 / total) : 0;

        saveTasks(studentId, vo);
        savePathRow(studentId, vo, progress);
        saveHistory(studentId, vo);
        log.info("[Path] 路径生成完成 studentId={}, totalTasks={}, goal={}", studentId, total, vo.getGoal());
        return vo;
    }

    @Override
    @Transactional
    public LearningPathVO updateTaskStatus(Long studentId, String stageName, String taskTitle, boolean completed) {
        List<LearningTask> tasks = taskMapper.selectByUserId(studentId);
        if (tasks == null || tasks.isEmpty()) {
            return getCurrentPath(studentId);
        }
        boolean found = false;
        for (LearningTask t : tasks) {
            boolean titleMatch = t.getTitle().equals(taskTitle);
            boolean stageMatch = stageName == null || stageName.isBlank()
                    || stageName.equals(t.getStage()) || stageName.equals(STAGE_NAME.get(t.getStage()));
            if (titleMatch && stageMatch) {
                t.setStatus(completed ? "done" : "todo");
                t.setProgress(completed ? 100 : 0);
                t.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(t);
                found = true;
                break;
            }
        }
        if (!found) {
            log.warn("[Path] 未找到要更新的任务: {}", taskTitle);
            return getCurrentPath(studentId);
        }

        int total = tasks.size();
        int done = (int) tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        int progress = total > 0 ? (int) Math.round(done * 100.0 / total) : 0;

        LearningPath lp = pathMapper.findActiveByStudentId(studentId);
        if (lp != null) {
            lp.setProgress(progress);
            lp.setUpdateTime(LocalDateTime.now());
            pathMapper.updateById(lp);
        }
        // 任务完成 → 发布 study.progress（教师端看板消费）
        progressPublisher.publish(studentId, progress, done, total);
        return getCurrentPath(studentId);
    }

    @Override
    public List<PathHistoryVO> getHistory(Long studentId) {
        return historyMapper.findByStudentId(studentId).stream().map(h -> {
            PathHistoryVO vo = new PathHistoryVO();
            vo.setId(h.getId());
            vo.setGoal(h.getGoal());
            if (h.getPathData() != null) {
                try {
                    vo.setPathData(objectMapper.readValue(h.getPathData(),
                            new TypeReference<Map<String, Object>>() { }));
                } catch (Exception ignored) { }
            }
            vo.setCreateTime(h.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    // ---------- 私有方法 ----------

    private LearningPathVO requestAiPath(Long studentId, Map<String, Object> profileMap) {
        String prompt = buildPrompt(profileMap);
        try {
            AiPathRequest req = AiPathRequest.builder()
                    .studentId(String.valueOf(studentId))
                    .prompt(prompt)
                    .profile(profileMap)
                    .build();
            Map<String, Object> data = aiResultParser.parsePathData(aiServiceClient.generatePath(req));
            if (data != null) {
                LearningPathVO vo = objectMapper.convertValue(data, LearningPathVO.class);
                if (vo.getStages() != null && !vo.getStages().isEmpty()) {
                    log.info("[Path] AI 路径解析成功: goal={}, stages={}", vo.getGoal(), vo.getStages().size());
                    return vo;
                }
            }
        } catch (Exception e) {
            log.error("[Path] AI 路径生成失败, 走 fallback: {}", e.getMessage());
        }
        return createFallbackPath();
    }

    private Map<String, Object> buildProfileMap(Long studentId) {
        Map<String, Object> profileMap = new LinkedHashMap<>();
        try {
            StudentProfile sp = profileMapper.findByStudentId(studentId);
            if (sp != null) {
                putIfNotNull(profileMap, "course", sp.getCourse());
                putIfNotNull(profileMap, "topic", sp.getTopic());
                putIfNotNull(profileMap, "pace", sp.getPace());
                putIfNotNull(profileMap, "learningGoal", sp.getLearningGoal());
                putIfNotNull(profileMap, "weaknesses", sp.getWeaknesses());
                putIfNotNull(profileMap, "knowledgeBase", sp.getKnowledgeBase());
                putIfNotNull(profileMap, "cognitiveStyle", sp.getCognitiveStyle());
                putIfNotNull(profileMap, "resourcePreference", sp.getResourcePreference());
            } else {
                log.warn("[Path] 未找到学生画像 studentId={}", studentId);
            }
        } catch (Exception e) {
            log.error("[Path] 查画像异常: {}", e.getMessage());
        }
        return profileMap;
    }

    private String buildPrompt(Map<String, Object> profile) {
        StringBuilder sb = new StringBuilder("学生画像信息：\n");
        appendIfNotNull(sb, "课程", profile.get("course"));
        appendIfNotNull(sb, "当前主题", profile.get("topic"));
        appendIfNotNull(sb, "学习节奏", profile.get("pace"));
        appendIfNotNull(sb, "学习目标", profile.get("learningGoal"));
        appendIfNotNull(sb, "薄弱点", profile.get("weaknesses"));
        appendIfNotNull(sb, "知识基础", profile.get("knowledgeBase"));
        appendIfNotNull(sb, "认知风格", profile.get("cognitiveStyle"));
        appendIfNotNull(sb, "资源偏好", profile.get("resourcePreference"));
        if (profile.isEmpty()) {
            sb.append("学生：新用户，暂无画像数据。\n");
        }
        return sb + "\n请根据以上学生画像，生成一份完整的学习路径规划。\n\n"
                + "要求返回纯 JSON（不要 markdown 代码块），字段：\n"
                + "{\n"
                + "  \"goal\": \"学习目标描述\",\n"
                + "  \"targetMastery\": \"目标掌握度如 ≥85%\",\n"
                + "  \"totalHours\": 总学习小时数(整数),\n"
                + "  \"stages\": [\n"
                + "    {\"name\": \"今日计划\", \"tasks\": [{\"title\": \"具体任务名称\", \"duration\": 30}]},\n"
                + "    {\"name\": \"本周路径\", \"tasks\": [...]},\n"
                + "    {\"name\": \"考试冲刺\", \"tasks\": [...]},\n"
                + "    {\"name\": \"实践提升\", \"tasks\": [...]}\n"
                + "  ],\n"
                + "  \"suggestions\": \"路径调整建议文字\",\n"
                + "  \"applicationAdvice\": \"应用建议文字\",\n"
                + "  \"examAdvice\": \"阶段测评建议文字\",\n"
                + "  \"masteryRate\": 72,\n"
                + "  \"recommendTime\": \"每天 19:00-21:00\"\n"
                + "}\n\n"
                + "重要：stages 必须有且仅有 4 个，name 严格为：今日计划、本周路径、考试冲刺、实践提升。\n"
                + "每个 stage 的 tasks 至少 2 个，title 必须是具体的知识点名称。\n"
                + "根据学生画像中的课程、主题、薄弱点生成贴切的任务名称。";
    }

    /** 从 learning_tasks 重建 stages（任务状态权威源） */
    private List<StageVO> buildStagesFromTasks(List<LearningTask> tasks) {
        Map<String, List<TaskVO>> grouped = new LinkedHashMap<>();
        if (tasks != null) {
            for (LearningTask t : tasks) {
                String name = STAGE_NAME.getOrDefault(
                        t.getStage() != null && !t.getStage().isBlank() ? t.getStage() : "today", "待办任务");
                TaskVO tv = new TaskVO();
                tv.setId(t.getId());
                tv.setTitle(t.getTitle());
                tv.setDescription(t.getDescription());
                tv.setDuration(t.getStartTime() != null && t.getEndTime() != null
                        ? (int) java.time.Duration.between(t.getStartTime(), t.getEndTime()).toMinutes() : 30);
                tv.setStatus(STATUS_CODE.getOrDefault(t.getStatus(), 0));
                tv.setProgress(t.getProgress() != null ? t.getProgress() : 0);
                tv.setCreateTime(t.getCreateTime());
                grouped.computeIfAbsent(name, k -> new ArrayList<>()).add(tv);
            }
        }
        List<StageVO> stages = new ArrayList<>();
        grouped.forEach((name, taskList) -> {
            StageVO s = new StageVO();
            s.setName(name);
            s.setTasks(taskList);
            stages.add(s);
        });
        return stages;
    }

    /** C9：自算补全 totalTasks/completedTasks/learningRate/unmasteredRate */
    private void fillAggregates(LearningPathVO vo) {
        if (vo.getStages() == null) {
            vo.setStages(new ArrayList<>());
        }
        int total = countTotal(vo);
        int done = countDone(vo);
        vo.setTotalTasks(total);
        vo.setCompletedTasks(done);
        int mastery = vo.getMasteryRate() != null ? vo.getMasteryRate()
                : (total > 0 ? (int) Math.round(done * 100.0 / total) : 0);
        vo.setMasteryRate(mastery);
        int doing = 0;
        for (StageVO s : vo.getStages()) {
            if (s.getTasks() != null) {
                doing += (int) s.getTasks().stream().filter(t -> t.getStatus() != null && t.getStatus() == 1).count();
            }
        }
        int learning = total > 0 ? (int) Math.round(doing * 100.0 / total) : 0;
        vo.setLearningRate(learning);
        vo.setUnmasteredRate(Math.max(0, 100 - mastery - learning));
        if (vo.getCompletedTasks() == null) vo.setCompletedTasks(done);
    }

    private void saveTasks(Long studentId, LearningPathVO vo) {
        // 重新生成 → 清理旧任务，防止重复
        taskMapper.delete(new LambdaQueryWrapper<LearningTask>().eq(LearningTask::getUserId, studentId));
        for (StageVO stage : vo.getStages()) {
            String stageKey = NAME_STAGE.getOrDefault(stage.getName(), "today");
            if (stage.getTasks() == null) continue;
            for (TaskVO t : stage.getTasks()) {
                LearningTask task = new LearningTask();
                task.setUserId(studentId);
                task.setTitle(t.getTitle());
                task.setDescription(t.getDescription());
                task.setStage(stageKey);
                task.setStatus(CODE_STATUS.getOrDefault(t.getStatus() != null ? t.getStatus() : 0, "todo"));
                task.setProgress(t.getProgress() != null ? t.getProgress() : 0);
                task.setPriority("middle");
                task.setCreateTime(LocalDateTime.now());
                task.setUpdateTime(LocalDateTime.now());
                taskMapper.insert(task);
                t.setId(task.getId()); // 落库 ID 回填
            }
        }
    }

    private void savePathRow(Long studentId, LearningPathVO vo, int progress) {
        try {
            LearningPath lp = new LearningPath();
            lp.setStudentId(studentId);
            lp.setSteps(objectMapper.writeValueAsString(vo));
            lp.setProgress(progress);
            lp.setPace("medium");
            lp.setGoal(vo.getGoal());
            lp.setSuggestions(vo.getSuggestions());
            lp.setRecommendations(vo.getApplicationAdvice());
            lp.setExamAdvice(vo.getExamAdvice());
            lp.setStatus("active");
            lp.setCreateTime(LocalDateTime.now());
            lp.setUpdateTime(LocalDateTime.now());
            pathMapper.upsert(lp);
        } catch (Exception e) {
            log.error("[Path] 路径落库失败: {}", e.getMessage(), e);
        }
    }

    private void saveHistory(Long studentId, LearningPathVO vo) {
        try {
            LearningPathHistory h = new LearningPathHistory();
            h.setStudentId(studentId);
            h.setGoal(vo.getGoal());
            h.setPathData(objectMapper.writeValueAsString(Map.of(
                    "snapshotTime", LocalDateTime.now().toString(),
                    "totalTasks", vo.getTotalTasks() != null ? vo.getTotalTasks() : 0,
                    "progress", 0)));
            h.setCreatedAt(LocalDateTime.now());
            historyMapper.insert(h);
        } catch (Exception e) {
            log.error("[Path] 历史写入失败: {}", e.getMessage(), e);
        }
    }

    private LearningPathVO parseStored(LearningPath lp) {
        if (lp == null || lp.getSteps() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(lp.getSteps(), LearningPathVO.class);
        } catch (Exception e) {
            log.warn("[Path] 解析 steps JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    private LearningPathVO createFallbackPath() {
        LearningPathVO vo = new LearningPathVO();
        vo.setGoal("完成课程学习目标");
        vo.setTargetMastery("≥85%");
        vo.setEstimatedCompletion(LocalDate.now().plusDays(30).toString());
        vo.setTotalHours(20);
        vo.setMasteryRate(50);
        vo.setSuggestions("暂无调整建议（AI 服务暂不可用）");
        vo.setApplicationAdvice("暂无应用建议");
        vo.setExamAdvice("暂无测评建议");
        vo.setRecommendTime("每天 19:00-21:00");
        List<StageVO> stages = new ArrayList<>();
        for (String name : List.of("今日计划", "本周路径", "考试冲刺", "实践提升")) {
            StageVO s = new StageVO();
            s.setName(name);
            TaskVO t1 = new TaskVO();
            t1.setTitle(name + " · 基础巩固练习");
            t1.setDuration(30);
            t1.setStatus(0);
            t1.setProgress(0);
            TaskVO t2 = new TaskVO();
            t2.setTitle(name + " · 拓展任务");
            t2.setDuration(45);
            t2.setStatus(0);
            t2.setProgress(0);
            s.setTasks(Arrays.asList(t1, t2));
            stages.add(s);
        }
        vo.setStages(stages);
        return vo;
    }

    private int countTotal(LearningPathVO vo) {
        return vo.getStages() == null ? 0
                : vo.getStages().stream().mapToInt(s -> s.getTasks() != null ? s.getTasks().size() : 0).sum();
    }

    private int countDone(LearningPathVO vo) {
        if (vo.getStages() == null) return 0;
        return vo.getStages().stream()
                .mapToInt(s -> s.getTasks() == null ? 0
                        : (int) s.getTasks().stream().filter(t -> t.getStatus() != null && t.getStatus() == 2).count())
                .sum();
    }

    private void putIfNotNull(Map<String, Object> m, String key, Object val) {
        if (val != null) m.put(key, val);
    }

    private void appendIfNotNull(StringBuilder sb, String label, Object val) {
        if (val != null && !String.valueOf(val).isBlank()) {
            sb.append("- ").append(label).append("：").append(val).append("\n");
        }
    }
}
