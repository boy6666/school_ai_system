package com.eduagent.service.impl;

import com.eduagent.agent.AiClient;
import com.eduagent.agent.AiChatResponse;
import com.eduagent.entity.Conversation;
import com.eduagent.mapper.ConversationMapper;
import com.eduagent.mapper.StudentProfileMapper;
import com.eduagent.mapper.QuizAnswerMapper;
import com.eduagent.mapper.LearningPathHistoryMapper;
import com.eduagent.mapper.LearningTaskMapper;
import com.eduagent.entity.StudentProfile;
import com.eduagent.entity.QuizAnswer;
import com.eduagent.entity.LearningPathHistory;
import com.eduagent.entity.LearningTask;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.service.TutorService;
import com.eduagent.vo.TutorReplyVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorServiceImpl implements TutorService {

    private final AiClient aiClient;
    private final ConversationMapper conversationMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final LearningPathHistoryMapper learningPathHistoryMapper;
    private final LearningTaskMapper learningTaskMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TutorReplyVO chat(Long studentId, String message, String sessionId) {
        log.info("智能辅导: studentId={}, message={}", studentId, message);

        // 从 MySQL 读取学生画像
        Map<String, Object> profileMap = new HashMap<>();
        try {
            StudentProfile sp = studentProfileMapper.findByStudentId(studentId);
            if (sp != null) {
                profileMap.put("course", sp.getCourse());
                profileMap.put("topic", sp.getTopic());
                profileMap.put("knowledge_base", sp.getKnowledgeBase());
                profileMap.put("weaknesses", sp.getWeaknesses());
                profileMap.put("pace", sp.getPace());
                profileMap.put("resource_preference", sp.getResourcePreference());
                profileMap.put("last_score", sp.getLastScore());
            }
        } catch (Exception e) {
            log.warn("读取画像失败: {}", e.getMessage());
        }

        // 查询最近错题（isCorrect=0，取最近5条）
        try {
            List<QuizAnswer> wrongAnswers = quizAnswerMapper.selectList(
                    new LambdaQueryWrapper<QuizAnswer>()
                            .eq(QuizAnswer::getStudentId, studentId)
                            .eq(QuizAnswer::getIsCorrect, 0)
                            .orderByDesc(QuizAnswer::getCreateTime)
                            .last("LIMIT 5")
            );
            if (!wrongAnswers.isEmpty()) {
                List<Map<String, Object>> wrongList = wrongAnswers.stream().map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("question", a.getQuestion());
                    m.put("userAnswer", a.getUserAnswer());
                    m.put("correctAnswer", a.getCorrectAnswer());
                    m.put("explanation", a.getExplanation());
                    return m;
                }).collect(Collectors.toList());
                profileMap.put("wrong_questions", wrongList);
                log.info("已加载 {} 道错题到 profile", wrongList.size());
            }
        } catch (Exception e) {
            log.warn("加载错题失败: {}", e.getMessage());
        }

        // 查询学习路径和任务进度
        try {
            List<LearningPathHistory> paths = learningPathHistoryMapper.findByStudentId(studentId);
            if (!paths.isEmpty()) {
                LearningPathHistory latest = paths.get(0);
                Map<String, Object> pathInfo = new HashMap<>();
                pathInfo.put("goal", latest.getGoal());
                pathInfo.put("pathData", latest.getPathData());
                profileMap.put("learning_path", pathInfo);
            }
            List<LearningTask> tasks = learningTaskMapper.selectByUserId(studentId);
            if (!tasks.isEmpty()) {
                long total = tasks.size();
                long completed = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("total", total);
                taskInfo.put("completed", completed);
                taskInfo.put("progress", total > 0 ? Math.round((float) completed / total * 100) : 0);
                profileMap.put("tasks", taskInfo);
            }
        } catch (Exception e) {
            log.warn("加载学习路径/任务数据失败: {}", e.getMessage());
        }

        // 调用 Python AI 引擎（带增强画像）
        AiChatResponse aiResp = aiClient.chat(String.valueOf(studentId), sessionId, message, profileMap);

        // AI 返回后，把画像变更写回 MySQL
        try {
            Map<String, Object> updatedProfile = aiResp.getProfile();
            if (updatedProfile != null && !updatedProfile.isEmpty()) {
                StudentProfile sp = studentProfileMapper.findByStudentId(studentId);
                if (sp == null) {
                    sp = new StudentProfile();
                    sp.setStudentId(studentId);
                }
                if (updatedProfile.containsKey("topic")) sp.setTopic((String) updatedProfile.get("topic"));
                if (updatedProfile.containsKey("course")) sp.setCourse((String) updatedProfile.get("course"));
                if (updatedProfile.containsKey("knowledge_base")) sp.setKnowledgeBase((String) updatedProfile.get("knowledge_base"));
                if (updatedProfile.containsKey("weaknesses")) sp.setWeaknesses((String) updatedProfile.get("weaknesses"));
                if (updatedProfile.containsKey("pace")) sp.setPace((String) updatedProfile.get("pace"));
                if (updatedProfile.containsKey("last_score")) sp.setLastScore((Integer) updatedProfile.get("last_score"));
                studentProfileMapper.insertOrUpdate(sp);
                log.info("画像已同步到 MySQL: studentId={}", studentId);
            }
        } catch (Exception e) {
            log.warn("画像写回 MySQL 失败: {}", e.getMessage());
        }

        // 保存对话记录
        Conversation conv = new Conversation();
        conv.setStudentId(studentId);
        conv.setSessionId(sessionId);
        conv.setQuestion(message);
        conv.setAnswer(aiResp.getFinalAnswer());
        conv.setIntent(aiResp.getIntent());

        try {
            if (aiResp.getEvaluationReport() != null) {
                conv.setEvaluationReport(objectMapper.writeValueAsString(aiResp.getEvaluationReport()));
            }
        } catch (Exception e) {
            log.warn("序列化评估报告失败", e);
        }

        conv.setResourceDir(aiResp.getResourceDir());
        conv.setCreateTime(java.time.LocalDateTime.now());
        conversationMapper.insert(conv);

        // 构建返回
        String evalSummary = "";
        if (aiResp.getEvaluationReport() != null) {
            Object score = aiResp.getEvaluationReport().get("understanding_score");
            evalSummary = score != null ? "掌握度: " + score : "";
        }

        return TutorReplyVO.builder()
                .answer(aiResp.getFinalAnswer())
                .intent(aiResp.getIntent())
                .routeReason(aiResp.getRouteReason())
                .evaluation(evalSummary)
                .resourceDir(aiResp.getResourceDir())
                .build();
    }

    @Override
    public List<Map<String, Object>> getSessions(Long studentId) {
        List<Map<String, Object>> sessions = new ArrayList<>();
        List<Conversation> convs = conversationMapper.selectByStudentId(studentId);
        Set<String> seen = new HashSet<>();
        for (Conversation c : convs) {
            if (c.getSessionId() != null && seen.add(c.getSessionId())) {
                Map<String, Object> s = new HashMap<>();
                s.put("sessionId", c.getSessionId());
                s.put("title", c.getQuestion() != null && c.getQuestion().length() > 20 ? c.getQuestion().substring(0, 20) + "..." : c.getQuestion());
                s.put("time", c.getCreateTime() != null ? c.getCreateTime().toString() : "");
                sessions.add(s);
            }
        }
        return sessions;
    }

    @Override
    public List<TutorReplyVO> getHistory(Long studentId, String sessionId) {
        List<Conversation> convs;
        if (sessionId != null && !sessionId.isEmpty()) {
            convs = conversationMapper.selectBySession(studentId, sessionId);
        } else {
            convs = conversationMapper.selectByStudentId(studentId);
        }

        return convs.stream().map(c -> TutorReplyVO.builder()
                .question(c.getQuestion())
                .answer(c.getAnswer())
                .intent(c.getIntent())
                .resourceDir(c.getResourceDir())
                .build()).collect(Collectors.toList());
    }
}
