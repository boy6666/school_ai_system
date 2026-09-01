package com.eduagent.teacher.controller;

import com.eduagent.common.result.Result;
import com.eduagent.teacher.dto.CreateAssignmentRequest;
import com.eduagent.teacher.dto.UpdateAssignmentRequest;
import com.eduagent.teacher.service.AssignmentService;
import com.eduagent.teacher.vo.AssignmentDetailVO;
import com.eduagent.teacher.vo.AssignmentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 作业管理（T）。作业发布经 /publish 触发 assignment.published 事件。
 */
@RestController
@RequestMapping("/api/edu-agent-teacher/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    public Result<AssignmentVO> create(@Valid @RequestBody CreateAssignmentRequest request) {
        return Result.success(assignmentService.create(request));
    }

    @GetMapping
    public Result<List<AssignmentVO>> list(@RequestParam(required = false) Long classId) {
        return Result.success(assignmentService.list(classId));
    }

    @GetMapping("/{id}")
    public Result<AssignmentDetailVO> detail(@PathVariable Long id) {
        return Result.success(assignmentService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<AssignmentVO> update(@PathVariable Long id, @Valid @RequestBody UpdateAssignmentRequest request) {
        return Result.success(assignmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/items")
    public Result<AssignmentVO> addItem(@PathVariable Long id, @Valid @RequestBody CreateAssignmentRequest.ItemReq item) {
        return Result.success(assignmentService.addItem(id, item));
    }

    @PostMapping("/{id}/publish")
    public Result<AssignmentVO> publish(@PathVariable Long id) {
        return Result.success(assignmentService.publish(id));
    }
}
