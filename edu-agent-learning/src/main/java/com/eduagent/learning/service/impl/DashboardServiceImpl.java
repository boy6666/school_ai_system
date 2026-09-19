package com.eduagent.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.learning.dto.ai.AiChatRequest;
import com.eduagent.learning.entity.LearningPath;
import com.eduagent.learning.entity.LearningTask;
import com.eduagent.learning.entity.Report;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.LearningPathMapper;
import com.eduagent.learning.mapper.LearningTaskMapper;
import com.eduagent.learning.mapper.ReportMapper;
import com.eduagent.learning.mapper.StudyLogMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.eduagent.learning.service.DashboardService;
import com.eduagent.learning.service.ProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard AI 能力（学习总结 / 学习回顾 / 六维评价）。
 * 沿用单体 prompt 思路；AI 调用统一走 /chat，review 不再反向依赖 resource 域（spec A.2.5 边界）。
 * 生成结果复用 report 表存储（title 前缀区分），GET 读最新一条。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String SUMMARY_TITLE_PREFIX = "AI学习总结";
    private static final String REVIEW_TITLE_PREFIX = "AI学习回顾";

    private final ReportMapper reportMapper;
    private final StudentProfileMapper profileMapper;
    private final LearningPathMapper pathMapper;
    private final LearningTaskMapper taskMapper;
    private final StudyLogMapper studyLogMapper;
    private final AiServiceClient aiServiceClient;
    private final AiResultParser aiResultParser;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> generateAiSummary(Long studentId) {
        log.info("[Dashboard] AI学习总结 studentId={}", studentId);

        StringBuilder data = new StringBuilder();
        appendProfile(data, studentId);
        appendPath(data, studentId);
        appendStudyDuration(data, studentId);
        appendTaskStats(data, studentId);

        String prompt = data + "\n请根据以上数据，生成一份学习总结。\n"
                + "要求返回 JSON（纯JSON，不要markdown）：\n"
                + "{\n"
                + "  \"summary\": \"总体学习总结，200字以内\",\n"
                + "  \"strengths\": \"做得好的方面，100字以内\",\n"
                + "  \"weaknessAnalysis\": \"需要改进的方面，100字以内\",\n"
                + "  \"suggestion\": \"下一步学习建议，100字以内\",\n"
                + "  \"score\": 综合评分(0-100整数),\n"
                + "  \"focusNext\": \"建议重点学习内容\"\n"
                + "}";

        Map<String, Object> result = callAiForJson(studentId, prompt);
        if (!result.containsKey("summary")) {
            result.put("summary", "数据分析中，请稍后再试");
            result.put("score", 0);
        }
        saveReport(studentId, SUMMARY_TITLE_PREFIX + " - " + LocalDate.now(), result);
        return result;
    }

    @Override
    public Map<String, Object> getLatestAiSummary(Long studentId) {
        return readLatestReport(studentId, SUMMARY_TITLE_PREFIX);
    }

    @Override
    public Map<String, Object> generateLearningReview(Long studentId) {
        log.info("[Dashboard] AI学习回顾 studentId={}", studentId);

        StringBuilder data = new StringBuilder();
        appendProfile(data, studentId);
        appendStudyDuration(data, studentId);
        appendTaskStats(data, studentId);

        String prompt = data + "\n请根据以上学习数据，生成一份学习回顾。\n"
                + "要求返回 JSON（纯JSON，不要markdown）：\n"
                + "{\n"
                + "  \"summary\": \"总体回顾，200字以内，描述学习了哪些内容\",\n"
                + "  \"completedContent\": \"已完成的学习内容总结，150字以内\",\n"
                + "  \"timeAnalysis\": \"时间分配分析，100字以内\",\n"
                + "  \"nextStep\": \"建议下一步学习方向，100字以内\"\n"
                + "}";

        Map<String, Object> result = callAiForJson(studentId, prompt);
        if (!result.containsKey("summary")) {
            result.put("summary", "暂无学习回顾数据");
            result.put("nextStep", "继续当前学习计划");
        }
        saveReport(studentId, REVIEW_TITLE_PREFIX + " - " + LocalDate.now(), result);
        return result;
    }

    @Override
    public Map<String, Object> getLatestLearningReview(Long studentId) {
        return readLatestReport(studentId, REVIEW_TITLE_PREFIX);
    }

    @Override
    public Map<String, Object> evaluation(Long studentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        StudentProfile sp = profileMapper.findByStudentId(studentId);
        if (sp == null) {
            return result;
        }
        result.put("score", sp.getLastScore() != null ? sp.getLastScore() : 0);
        result.put("learning_goal", sp.getLearningGoal());
        result.put("cognitive_style", sp.getCognitiveStyle());
        result.put("pace", sp.getPace());
        result.put("course", sp.getCourse());
        result.put("topic", sp.getTopic());
        if (sp.getProfileData() != null) {
            try {
                result.putAll(objectMapper.readValue(sp.getProfileData(),
                        new TypeReference<Map<String, Object>>() { }));
            } catch (Exception ignored) { }
        }
        result.put("weaknesses", profileService.parseJsonArray(sp.getWeaknesses()));
        return result;
    }

    // ---------- 私有方法 ----------

    /** AI /chat → finalAnswer 中提取 JSON（宽容解析，失败返回空 Map） */
    private Map<String, Object> callAiForJson(Long studentId, String prompt) {
        try {
            AiChatRequest req = AiChatRequest.of(prompt, studentId,
                    "dashboard_" + studentId + "_" + System.currentTimeMillis(), null);
            var result = aiResultParser.parseChatResult(aiServiceClient.chat(req));
            if (result != null && result.getFinalAnswer() != null) {
                Map<String, Object> json = aiResultParser.parseEmbeddedJson(result.getFinalAnswer());
                if (json != null) return json;
                return Map.of("summary", result.getFinalAnswer());
            }
        } catch (Exception e) {
            log.warn("[Dashboard] AI 调用失败: {}", e.getMessage());
        }
        return new LinkedHashMap<>();
    }

    private void saveReport(Long studentId, String title, Map<String, Object> content) {
        try {
            Report report = new Report();
            report.setStudentId(studentId);
            report.setTitle(title);
            report.setContent(objectMapper.writeValueAsString(content));
            report.setPeriodStart(LocalDate.now().minusDays(7));
            report.setPeriodEnd(LocalDate.now());
            report.setCreateTime(LocalDateTime.now());
            reportMapper.insert(report);
        } catch (Exception e) {
            log.error("[Dashboard] 报告落库失败: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> readLatestReport(Long studentId, String titlePrefix) {
        Report report = reportMapper.selectOne(new LambdaQueryWrapper<Report>()
                .eq(Report::getStudentId, studentId)
                .likeRight(Report::getTitle, titlePrefix)
                .orderByDesc(Report::getCreateTime)
                .last("LIMIT 1"));
        if (report != null && report.getContent() != null) {
            try {
                return objectMapper.readValue(report.getContent(),
                        new TypeReference<Map<String, Object>>() { });
            } catch (Exception e) {
                log.warn("[Dashboard] 解析已存报告失败: {}", e.getMessage());
            }
        }
        return new LinkedHashMap<>();
    }

    private void appendProfile(StringBuilder sb, Long studentId) {
        StudentProfile sp = profileMapper.findByStudentId(studentId);
        if (sp == null) return;
        sb.append("【学生画像】\n");
        line(sb, "课程", sp.getCourse());
        line(sb, "当前主题", sp.getTopic());
        line(sb, "学习节奏", sp.getPace());
        line(sb, "学习目标", sp.getLearningGoal());
        line(sb, "知识基础", sp.getKnowledgeBase());
        line(sb, "薄弱点", sp.getWeaknesses());
        line(sb, "认知风格", sp.getCognitiveStyle());
        line(sb, "综合类型", sp.getOverallType());
        line(sb, "最近评分", sp.getLastScore());
    }

    private void appendPath(StringBuilder sb, Long studentId) {
        LearningPath lp = pathMapper.findActiveByStudentId(studentId);
        if (lp == null) return;
        sb.append("\n【学习路径】\n");
        line(sb, "目标", lp.getGoal());
        line(sb, "进度", lp.getProgress() != null ? lp.getProgress() + "%" : "0%");
        line(sb, "建议", lp.getSuggestions());
    }

    private void appendStudyDuration(StringBuilder sb, Long studentId) {
        sb.append("\n【学习时长统计】\n");
        Integer totalSec = studyLogMapper.totalDuration(studentId);
        int hours = totalSec != null ? totalSec / 3600 : 0;
        int minutes = totalSec != null ? (totalSec % 3600) / 60 : 0;
        sb.append("总学习时长：").append(hours).append("小时").append(minutes).append("分钟\n");
        List<Map<String, Object>> modules = studyLogMapper.moduleSummary(studentId);
        if (modules != null) {
            for (Map<String, Object> m : modules) {
                long min = m.get("total") != null ? Long.parseLong(m.get("total").toString()) / 60 : 0;
                sb.append("  - ").append(m.get("module")).append("：").append(min).append("分钟\n");
            }
        }
    }

    private void appendTaskStats(StringBuilder sb, Long studentId) {
        List<LearningTask> all = taskMapper.selectByUserId(studentId);
        sb.append("\n【任务完成】\n");
        if (all == null || all.isEmpty()) {
            sb.append("暂无学习任务\n");
            return;
        }
        long done = all.stream().filter(t -> "done".equals(t.getStatus())).count();
        sb.append("总任务数：").append(all.size()).append("，已完成：").append(done).append("\n");
        for (LearningTask t : all) {
            if ("done".equals(t.getStatus())) {
                sb.append("  ✅ ").append(t.getTitle()).append("\n");
            }
        }
    }

    private void line(StringBuilder sb, String label, Object val) {
        if (val != null) {
            sb.append(label).append("：").append(val).append("\n");
        }
    }
}
