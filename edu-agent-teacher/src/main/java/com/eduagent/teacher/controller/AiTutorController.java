package com.eduagent.teacher.controller;

import com.eduagent.common.result.Result;
import com.eduagent.teacher.dto.AiAskRequest;
import com.eduagent.teacher.dto.AiExplainGradeRequest;
import com.eduagent.teacher.service.AiTutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 助教（T）。经 AiServiceClient 调 ai，透传教师身份。
 */
@RestController
@RequestMapping("/api/edu-agent-teacher/ai")
@RequiredArgsConstructor
public class AiTutorController {

    private final AiTutorService aiTutorService;

    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@Valid @RequestBody AiAskRequest request) {
        return Result.success(aiTutorService.ask(request));
    }

    @PostMapping("/explain-grade")
    public Result<Map<String, Object>> explainGrade(@Valid @RequestBody AiExplainGradeRequest request) {
        return Result.success(aiTutorService.explainGrade(request.getStudentId(), request.getAssignmentId()));
    }
}
