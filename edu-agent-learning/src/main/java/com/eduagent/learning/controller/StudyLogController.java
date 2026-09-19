package com.eduagent.learning.controller;

import com.eduagent.common.result.Result;
import com.eduagent.learning.common.RoleGuard;
import com.eduagent.learning.dto.StudyLogRequest;
import com.eduagent.learning.service.StudyLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 学习日志（看板数据源，module：mindmap/quiz/reading/code）。 */
@Slf4j
@RestController
@RequestMapping("/api/edu-agent-learning/study-log")
@RequiredArgsConstructor
public class StudyLogController {

    private final StudyLogService studyLogService;

    @PostMapping
    public Result<Map<String, Object>> add(@Valid @RequestBody StudyLogRequest request) {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(studyLogService.addLog(studentId, request));
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(studyLogService.summary(studentId));
    }

    @GetMapping("/report")
    public Result<Map<String, Object>> report() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(studyLogService.report(studentId));
    }

    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> tasks() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(studyLogService.pendingTasks(studentId));
    }

    @GetMapping("/path")
    public Result<Map<String, Object>> path() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(studyLogService.pathSummary(studentId));
    }
}
