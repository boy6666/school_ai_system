package com.eduagent.teacher.controller;

import com.eduagent.common.result.Result;
import com.eduagent.teacher.dto.AddStudentRequest;
import com.eduagent.teacher.dto.CreateClassRequest;
import com.eduagent.teacher.dto.UpdateClassRequest;
import com.eduagent.teacher.service.ClassService;
import com.eduagent.teacher.vo.ClassStudentVO;
import com.eduagent.teacher.vo.ClassVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 班级管理（全部 ROLE_TEACHER；属主校验在 service）。路径与网关 /api/edu-agent-teacher/** 对齐。
 */
@RestController
@RequestMapping("/api/edu-agent-teacher/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @PostMapping
    public Result<ClassVO> create(@Valid @RequestBody CreateClassRequest request) {
        return Result.success(classService.create(request));
    }

    @GetMapping
    public Result<List<ClassVO>> list() {
        return Result.success(classService.list());
    }

    @GetMapping("/{id}")
    public Result<ClassVO> get(@PathVariable Long id) {
        return Result.success(classService.get(id));
    }

    @PutMapping("/{id}")
    public Result<ClassVO> update(@PathVariable Long id, @Valid @RequestBody UpdateClassRequest request) {
        return Result.success(classService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/students")
    public Result<Void> addStudent(@PathVariable Long id, @Valid @RequestBody AddStudentRequest request) {
        classService.addStudent(id, request.getStudentId());
        return Result.success();
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public Result<Void> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        classService.removeStudent(id, studentId);
        return Result.success();
    }

    @GetMapping("/{id}/students")
    public Result<List<ClassStudentVO>> listStudents(@PathVariable Long id) {
        return Result.success(classService.listStudents(id));
    }
}
