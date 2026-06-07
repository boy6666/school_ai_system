package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.dto.TutorRequest;
import com.eduagent.service.TutorService;
import com.eduagent.vo.TutorReplyVO;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tutor")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> getSessions() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(tutorService.getSessions(userId));
    }

    @PostMapping("/chat")
    public Result<TutorReplyVO> chat(@Valid @RequestBody TutorRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(tutorService.chat(userId, request.getMessage(), request.getSessionId()));
    }

    @GetMapping("/history")
    public Result<List<TutorReplyVO>> getHistory(@RequestParam(required = false) String sessionId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(tutorService.getHistory(userId, sessionId));
    }
}
