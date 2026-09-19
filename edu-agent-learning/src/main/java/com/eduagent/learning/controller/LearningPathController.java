package com.eduagent.learning.controller;

import com.eduagent.common.result.Result;
import com.eduagent.learning.common.RoleGuard;
import com.eduagent.learning.dto.UpdateTaskRequest;
import com.eduagent.learning.service.LearningPathService;
import com.eduagent.learning.vo.LearningPathVO;
import com.eduagent.learning.vo.PathHistoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 学习路径（AI 生成 + 任务进度）。 */
@Slf4j
@RestController
@RequestMapping("/api/edu-agent-learning/path")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @GetMapping("/current")
    public Result<LearningPathVO> current() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(learningPathService.getCurrentPath(studentId));
    }

    @PostMapping("/generate")
    public Result<LearningPathVO> generate() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(learningPathService.generatePath(studentId));
    }

    @PutMapping("/task")
    public Result<LearningPathVO> updateTask(@Valid @RequestBody UpdateTaskRequest request) {
        Long studentId = RoleGuard.currentStudentId();
        boolean completed = request.getCompleted();
        return Result.success(learningPathService.updateTaskStatus(
                studentId, request.getTaskId(), request.getStageName(), request.getTaskTitle(), completed));
    }

    @GetMapping("/history")
    public Result<List<PathHistoryVO>> history() {
        Long studentId = RoleGuard.currentStudentId();
        return Result.success(learningPathService.getHistory(studentId));
    }
}
