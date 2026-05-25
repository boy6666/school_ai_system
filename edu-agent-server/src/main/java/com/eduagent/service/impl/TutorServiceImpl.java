package com.eduagent.service.impl;

import com.eduagent.agent.AiClient;
import com.eduagent.agent.AiChatResponse;
import com.eduagent.entity.Conversation;
import com.eduagent.mapper.ConversationMapper;
import com.eduagent.service.TutorService;
import com.eduagent.vo.TutorReplyVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorServiceImpl implements TutorService {

    private final AiClient aiClient;
    private final ConversationMapper conversationMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TutorReplyVO chat(Long studentId, String message, String sessionId) {
        log.info("智能辅导: studentId={}, message={}", studentId, message);

        // 调用 Python AI 引擎
        AiChatResponse aiResp = aiClient.chat(String.valueOf(studentId), sessionId, message);

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
    public List<TutorReplyVO> getHistory(Long studentId, String sessionId) {
        List<Conversation> convs;
        if (sessionId != null && !sessionId.isEmpty()) {
            convs = conversationMapper.selectBySession(studentId, sessionId);
        } else {
            convs = conversationMapper.selectByStudentId(studentId);
        }

        return convs.stream().map(c -> TutorReplyVO.builder()
                .answer(c.getAnswer())
                .intent(c.getIntent())
                .resourceDir(c.getResourceDir())
                .build()).collect(Collectors.toList());
    }
}
