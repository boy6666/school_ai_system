package com.eduagent.teacher.controller;

import com.eduagent.common.result.Result;
import com.eduagent.teacher.dto.SubmitAssignmentRequest;
import com.eduagent.teacher.dto.UpdateGradeRequest;
import com.eduagent.teacher.service.GradeService;
import com.eduagent.teacher.vo.GradeDetailVO;
import com.eduagent.teacher.vo.GradeVO;
import com.eduagent.teacher.vo.StudentAssignmentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提交与批改（核心跨服务）。submit 供学生；grades 供教师复核/查看。
 */
@RestController
@RequestMapping("/api/edu-agent-teacher")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @PostMapping("/assignments/{id}/submit")
    public Result<List<GradeVO>> submit(@PathVariable("id") Long assignmentId,
                                        @Valid @RequestBody SubmitAssignmentRequest request) {
        return Result.success(gradeService.submit(assignmentId, request));
    }

    @GetMapping("/assignments/{id}/grades")
    public Result<List<GradeVO>> listGrades(@PathVariable("id") Long assignmentId,
                                            @RequestParam(required = false) Long studentId) {
        return Result.success(gradeService.listGrades(assignmentId, studentId));
    }

    @GetMapping("/grades/{gradeId}")
    public Result<GradeDetailVO> gradeDetail(@PathVariable Long gradeId) {
        return Result.success(gradeService.getGrade(gradeId));
    }

    @PutMapping("/grades/{gradeId}")
    public Result<GradeVO> updateGrade(@PathVariable Long gradeId, @Valid @RequestBody UpdateGradeRequest request) {
        return Result.success(gradeService.updateGrade(gradeId, request));
    }

    @GetMapping("/students/{studentId}/assignments")
    public Result<List<StudentAssignmentVO>> studentAssignments(@PathVariable Long studentId) {
        return Result.success(gradeService.studentAssignments(studentId));
    }
}
