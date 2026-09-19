package com.eduagent.learning.controller;

import com.eduagent.common.result.Result;
import com.eduagent.learning.common.RoleGuard;
import com.eduagent.learning.dto.JudgeRequest;
import com.eduagent.learning.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 测验（作答记录 / 错题本 / AI 判分）。 */
@Slf4j
@RestController
@RequestMapping("/api/edu-agent-learning/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/answered")
    public Result<List<Map<String, Object>>> answered(
            @RequestParam(name = "resourceId", required = false) Long resourceId) {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(quizService.answered(studentId, resourceId));
    }

    @GetMapping("/wrong-questions")
    public Result<List<Map<String, Object>>> wrongQuestions() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(quizService.wrongQuestions(studentId));
    }

    @GetMapping("/wrong-questions/{id}")
    public Result<Map<String, Object>> wrongQuestionDetail(@PathVariable("id") Long id) {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(quizService.wrongQuestionDetail(studentId, id));
    }

    @PostMapping("/judge")
    public Result<Map<String, Object>> judge(@Valid @RequestBody JudgeRequest request) {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(quizService.judge(studentId, request));
    }
}
