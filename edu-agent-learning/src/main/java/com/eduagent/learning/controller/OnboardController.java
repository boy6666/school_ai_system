package com.eduagent.learning.controller;

import com.eduagent.common.result.Result;
import com.eduagent.learning.common.RoleGuard;
import com.eduagent.learning.dto.OnboardRequest;
import com.eduagent.learning.service.TutorService;
import com.eduagent.learning.vo.AiChatResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 引导对话（画像采集）。转发 edu-agent-ai /chat，learning 负责画像落库与对话留存。
 */
@Slf4j
@RestController
@RequestMapping("/api/edu-agent-learning/onboard")
@RequiredArgsConstructor
public class OnboardController {

    private final TutorService tutorService;

    @PostMapping("/chat")
    public Result<AiChatResult> chat(@Valid @RequestBody OnboardRequest request) {
        Long studentId = RoleGuard.currentStudentId();
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank()
                ? "onboard_" + System.currentTimeMillis()
                : request.getSessionId();
        return Result.success(tutorService.onboardChat(studentId, request.getMessage(), sessionId, request.getProfile()));
    }
}
