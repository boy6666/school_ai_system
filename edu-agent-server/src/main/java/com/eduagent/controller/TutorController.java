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

@RestController
@RequestMapping("/tutor")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;

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
