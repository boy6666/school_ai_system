package com.eduagent.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.learning.dto.ai.AiChatRequest;
import com.eduagent.learning.entity.Conversation;
import com.eduagent.learning.entity.LearningTask;
import com.eduagent.learning.entity.QuizAnswer;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.ConversationMapper;
import com.eduagent.learning.mapper.LearningPathHistoryMapper;
import com.eduagent.learning.mapper.LearningTaskMapper;
import com.eduagent.learning.mapper.QuizAnswerMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.eduagent.learning.service.ProfileService;
import com.eduagent.learning.service.TutorService;
import com.eduagent.learning.vo.AiChatResult;
import com.eduagent.learning.vo.TutorReplyVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 智能辅导 / 引导对话。画像组装与 AI 回写均在本服务；
 * 画像落库统一走 ProfileService.mergeAiProfile（唯一落库方，避免双写漂移）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TutorServiceImpl implements TutorService {

    private final AiServiceClient aiServiceClient;
    private final AiResultParser aiResultParser;
    private final ConversationMapper conversationMapper;
    private final StudentProfileMapper profileMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final LearningPathHistoryMapper pathHistoryMapper;
    private final LearningTaskMapper learningTaskMapper;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TutorReplyVO chat(Long studentId, String message, String sessionId) {
        log.info("[Tutor] 对话: studentId={}, sessionId={}", studentId, sessionId);
        Map<String, Object> profileMap = buildEnhancedProfile(studentId);

        AiChatResult ai;
        try {
            AiChatRequest req = AiChatRequest.builder()
                    .userInput(message)
                    .studentId(String.valueOf(studentId))
                    .sessionId(sessionId)
                    .profile(profileMap)
                    .build();
            ai = aiResultParser.parseChatResult(aiServiceClient.chat(req));
        } catch (Exception e) {
            log.error("[Tutor] AI 调用失败: {}", e.getMessage());
            ai = null;
        }

        String answer;
        if (ai != null && ai.getFinalAnswer() != null) {
            answer = ai.getFinalAnswer();
            // AI 回传画像 → 合并落库（辅导过程不置 profile_complete）
            profileService.mergeAiProfile(studentId, ai.getProfile(), false);
        } else {
            answer = "智能辅导暂时不可用，请稍后重试。你也可以先回顾错题本中的内容。";
        }

        saveConversation(studentId, sessionId, message, ai, answer);

        return TutorReplyVO.builder()
                .question(message)
                .answer(answer)
                .intent(ai != null ? ai.getIntent() : null)
                .routeReason(ai != null ? ai.getRouteReason() : null)
                .evaluation(evaluationSummary(ai))
                .resourceDir(ai != null ? ai.getResourceDir() : null)
                .build();
    }

    @Override
    @Transactional
    public AiChatResult onboardChat(Long studentId, String message, String sessionId,
                                    Map<String, Object> collectedProfile) {
        log.info("[Onboard] 引导对话: studentId={}, sessionId={}", studentId, sessionId);

        // 前端已采集画像优先，缺省则读库内画像
        Map<String, Object> profileMap = collectedProfile != null && !collectedProfile.isEmpty()
                ? new HashMap<>(collectedProfile)
                : buildBaseProfile(studentId);

        AiChatResult ai;
        try {
            AiChatRequest req = AiChatRequest.builder()
                    .userInput(message)
                    .studentId(String.valueOf(studentId))
                    .sessionId(sessionId)
                    .profile(profileMap)
                    .build();
            ai = aiResultParser.parseChatResult(aiServiceClient.chat(req));
        } catch (Exception e) {
            log.error("[Onboard] AI 调用失败: {}", e.getMessage());
            ai = null;
        }

        boolean done = ai != null && Boolean.TRUE.equals(ai.getProfileComplete());
        if (ai != null) {
            profileService.mergeAiProfile(studentId, ai.getProfile(), done);
        }

        String answer = ai != null && ai.getFinalAnswer() != null
                ? ai.getFinalAnswer()
                : "我是你的学习引导助手。可以先告诉我：你的专业、学习目标，以及目前对这门课的了解程度～";
        saveConversation(studentId, sessionId, message, ai, answer);

        AiChatResult out = new AiChatResult();
        out.setFinalAnswer(answer);
        out.setIntent(ai != null ? ai.getIntent() : "onboard");
        out.setProfile(ai != null ? ai.getProfile() : null);
        out.setProfileComplete(done);
        out.setResourceDir(ai != null ? ai.getResourceDir() : null);
        return out;
    }

    @Override
    public List<Map<String, Object>> getSessions(Long studentId) {
        List<Map<String, Object>> sessions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Conversation c : conversationMapper.selectByStudentId(studentId)) {
            if (c.getSessionId() != null && seen.add(c.getSessionId())) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("sessionId", c.getSessionId());
                s.put("title", c.getQuestion() != null && c.getQuestion().length() > 20
                        ? c.getQuestion().substring(0, 20) + "..." : c.getQuestion());
                s.put("time", c.getCreateTime() != null ? c.getCreateTime().toString() : "");
                sessions.add(s);
            }
        }
        return sessions;
    }

    @Override
    public List<TutorReplyVO> getHistory(Long studentId, String sessionId) {
        List<Conversation> convs = sessionId != null && !sessionId.isBlank()
                ? conversationMapper.selectBySession(studentId, sessionId)
                : conversationMapper.selectByStudentId(studentId);
        return convs.stream()
                .map(c -> TutorReplyVO.builder()
                        .question(c.getQuestion())
                        .answer(c.getAnswer())
                        .intent(c.getIntent())
                        .resourceDir(c.getResourceDir())
                        .build())
                .collect(Collectors.toList());
    }

    // ---------- 私有方法 ----------

    /** 基础画像（onboard 用） */
    private Map<String, Object> buildBaseProfile(Long studentId) {
        Map<String, Object> m = new HashMap<>();
        StudentProfile sp = profileMapper.findByStudentId(studentId);
        if (sp != null) {
            putIfNotNull(m, "course", sp.getCourse());
            putIfNotNull(m, "topic", sp.getTopic());
            putIfNotNull(m, "knowledgeBase", sp.getKnowledgeBase());
            putIfNotNull(m, "weaknesses", sp.getWeaknesses());
            putIfNotNull(m, "pace", sp.getPace());
            putIfNotNull(m, "resourcePreference", sp.getResourcePreference());
            putIfNotNull(m, "lastScore", sp.getLastScore());
        }
        return m;
    }

    /** 增强画像：基础画像 + 最近 5 道错题 + 路径进度（沿用单体逻辑，key 改 camelCase 契约） */
    private Map<String, Object> buildEnhancedProfile(Long studentId) {
        Map<String, Object> profileMap = buildBaseProfile(studentId);
        try {
            List<QuizAnswer> wrongAnswers = quizAnswerMapper.selectList(
                    new LambdaQueryWrapper<QuizAnswer>()
                            .eq(QuizAnswer::getStudentId, studentId)
                            .eq(QuizAnswer::getIsCorrect, 0)
                            .orderByDesc(QuizAnswer::getCreateTime)
                            .last("LIMIT 5"));
            if (!wrongAnswers.isEmpty()) {
                profileMap.put("wrongQuestions", wrongAnswers.stream().map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("question", a.getQuestion());
                    m.put("userAnswer", a.getUserAnswer());
                    m.put("correctAnswer", a.getCorrectAnswer());
                    m.put("explanation", a.getExplanation());
                    return m;
                }).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.warn("[Tutor] 加载错题失败: {}", e.getMessage());
        }
        try {
            var histories = pathHistoryMapper.findByStudentId(studentId);
            if (!histories.isEmpty()) {
                Map<String, Object> pathInfo = new LinkedHashMap<>();
                pathInfo.put("goal", histories.get(0).getGoal());
                pathInfo.put("pathData", histories.get(0).getPathData());
                profileMap.put("learningPath", pathInfo);
            }
            List<LearningTask> tasks = learningTaskMapper.selectByUserId(studentId);
            if (!tasks.isEmpty()) {
                long total = tasks.size();
                long completed = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
                Map<String, Object> taskInfo = new LinkedHashMap<>();
                taskInfo.put("total", total);
                taskInfo.put("completed", completed);
                taskInfo.put("progress", total > 0 ? Math.round((float) completed / total * 100) : 0);
                profileMap.put("tasks", taskInfo);
            }
        } catch (Exception e) {
            log.warn("[Tutor] 加载路径/任务失败: {}", e.getMessage());
        }
        return profileMap;
    }

    private void saveConversation(Long studentId, String sessionId, String question,
                                  AiChatResult ai, String answer) {
        try {
            Conversation conv = new Conversation();
            conv.setStudentId(studentId);
            conv.setSessionId(sessionId);
            conv.setQuestion(question);
            conv.setAnswer(answer);
            conv.setIntent(ai != null ? ai.getIntent() : null);
            if (ai != null && ai.getIntentConfidence() != null) {
                conv.setIntentConfidence(String.valueOf(ai.getIntentConfidence()));
            }
            if (ai != null && ai.getEvaluationReport() != null) {
                conv.setEvaluationReport(objectMapper.writeValueAsString(ai.getEvaluationReport()));
            }
            if (ai != null) {
                conv.setResourceDir(ai.getResourceDir());
            }
            conv.setCreateTime(LocalDateTime.now());
            conversationMapper.insert(conv);
        } catch (Exception e) {
            log.error("[Tutor] 对话留存失败: {}", e.getMessage(), e);
        }
    }

    private String evaluationSummary(AiChatResult ai) {
        if (ai == null || ai.getEvaluationReport() == null) {
            return "";
        }
        Object score = ai.getEvaluationReport().get("understanding_score");
        if (score == null) {
            score = ai.getEvaluationReport().get("understandingScore");
        }
        return score != null ? "掌握度: " + score : "";
    }

    private void putIfNotNull(Map<String, Object> m, String key, Object val) {
        if (val != null) {
            m.put(key, val);
        }
    }
}
