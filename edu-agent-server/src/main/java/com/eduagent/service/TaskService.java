package com.eduagent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.dto.TaskCreateRequest;
import com.eduagent.dto.TaskUpdateRequest;
import com.eduagent.vo.TaskVO;

public interface TaskService {
    TaskVO createTask(Long studentId, TaskCreateRequest req);
    TaskVO updateTask(Long taskId, TaskUpdateRequest req);
    void deleteTask(Long taskId, Long studentId);
    TaskVO getTask(Long taskId, Long studentId);
    Page<TaskVO> listTasks(Long studentId, Integer page, Integer size, Integer status);
}
