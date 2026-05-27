package com.eduagent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.common.Result;
import com.eduagent.dto.TaskCreateRequest;
import com.eduagent.dto.TaskUpdateRequest;
import com.eduagent.security.SecurityUtils;
import com.eduagent.service.TaskService;
import com.eduagent.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @PostMapping
    public Result<TaskVO> createTask(@RequestBody TaskCreateRequest req) {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(taskService.createTask(studentId, req));
    }

    @PutMapping("/{id}")
    public Result<TaskVO> updateTask(@PathVariable Long id, @RequestBody TaskUpdateRequest req) {
        req.setId(id);
        req.setStudentId(SecurityUtils.getCurrentUserId());
        return Result.success(taskService.updateTask(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        Long studentId = SecurityUtils.getCurrentUserId();
        taskService.deleteTask(id, studentId);
        return Result.success(null);
    }

    @GetMapping("/{id}")
    public Result<TaskVO> getTask(@PathVariable Long id) {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(taskService.getTask(id, studentId));
    }

    @GetMapping
    public Result<Page<TaskVO>> listTasks(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) Integer status) {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(taskService.listTasks(studentId, page, size, status));
    }
}
