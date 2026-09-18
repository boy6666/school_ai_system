package com.eduagent.learning.controller;

import com.eduagent.common.result.Result;
import com.eduagent.learning.common.RoleGuard;
import com.eduagent.learning.dto.TutorRequest;
import com.eduagent.learning.service.TutorService;
import com.eduagent.learning.vo.TutorReplyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 智能辅导（对话历史留存于 conversation 表）。 */
@Slf4j
@RestController
@RequestMapping("/api/edu-agent-learning/tutor")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        Long studentId = RoleGuard.currentUserId();
        return Result.success(tutorService.getSessions(studentId));
    }

    @PostMapping("/chat")
    public Result<TutorReplyVO> chat(@Valid @RequestBody TutorRequest request) {
        Long studentId = RoleGuard.currentUserId();
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank()
                ? "tutor_" + studentId + "_" + System.currentTimeMillis()
                : request.getSessionId();
        return Result.success(tutorService.chat(studentId, request.getMessage(), sessionId));
    }

    @GetMapping("/history")
    public Result<List<TutorReplyVO>> history(@RequestParam(required = false) String sessionId) {
        Long studentId = RoleGuard.currentUserId();
        return Result.success(tutorService.getHistory(studentId, sessionId));
    }
}
