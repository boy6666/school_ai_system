package com.eduagent.teacher.service.impl;

import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.teacher.dto.AiAskRequest;
import com.eduagent.teacher.dto.AiChatRequest;
import com.eduagent.teacher.dto.AiResourceRequest;
import com.eduagent.teacher.feign.AiServiceClient;
import com.eduagent.teacher.service.AiTutorService;
import com.eduagent.teacher.vo.AiChatResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTutorServiceImpl implements AiTutorService {

    private final AiServiceClient aiClient;

    @Override
    public Map<String, Object> ask(AiAskRequest request) {
        AiChatRequest chat = new AiChatRequest();
        chat.setMessage(request.getMessage());
        chat.setContext(request.getContext());
        try {
            AiChatResult r = aiClient.chat(chat).getData();
            if (r == null) {
                return Map.of("answer", "AI 未返回可用结果", "intent", "error", "references", null);
            }
            return Map.of("answer", r.answer() == null ? "" : r.answer(),
                    "intent", r.intent() == null ? "" : r.intent(),
                    "references", r.references());
        } catch (Exception e) {
            log.warn("AI 答疑失败: {}", e.getMessage());
            return Map.of("answer", "AI 服务暂不可用，请稍后重试", "intent", "error", "references", null);
        }
    }

    @Override
    public Map<String, Object> explainGrade(Long studentId, Long assignmentId) {
        AiResourceRequest req = AiResourceRequest.builder()
                .mode("evaluation")
                .extra(Map.of("studentId", studentId, "assignmentId", assignmentId))
                .build();
        try {
            Map<String, Object> data = aiClient.generate(req).getData();
            return data != null ? data : Map.of("analysis", "未生成分析");
        } catch (Exception e) {
            log.warn("AI 成绩解读失败: {}", e.getMessage());
            throw new ApiException(ErrorCode.SYSTEM_ERROR, "AI 解读服务不可用");
        }
    }
}
