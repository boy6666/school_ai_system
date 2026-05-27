package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.common.BusinessException;
import com.eduagent.dto.TaskCreateRequest;
import com.eduagent.dto.TaskUpdateRequest;
import com.eduagent.entity.Task;
import com.eduagent.mapper.TaskMapper;
import com.eduagent.service.TaskService;
import com.eduagent.vo.TaskVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService {
    @Autowired
    private TaskMapper taskMapper;

    @Override
    public TaskVO createTask(Long studentId, TaskCreateRequest req) {
        Task task = new Task();
        task.setStudentId(studentId);
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setDueDate(req.getDueDate());
        task.setPriority(req.getPriority());
        task.setStatus(0);
        taskMapper.insert(task);
        return toVO(task);
    }

    @Override
    public TaskVO updateTask(Long taskId, TaskUpdateRequest req) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        // 暂时不校验 studentId
        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getDueDate() != null) task.setDueDate(req.getDueDate());
        if (req.getPriority() != null) task.setPriority(req.getPriority());
        if (req.getStatus() != null) task.setStatus(req.getStatus());
        taskMapper.updateById(task);
        return toVO(task);
    }

    @Override
    public void deleteTask(Long taskId, Long studentId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        // 暂时不校验 studentId，直接删除
        taskMapper.deleteById(taskId);
    }

    @Override
    public TaskVO getTask(Long taskId, Long studentId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        return toVO(task);
    }

    @Override
    public Page<TaskVO> listTasks(Long studentId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                // 暂时不校验 studentId，查询所有任务
                .orderByDesc(Task::getPriority).orderByAsc(Task::getDueDate);
        if (status != null) {
            wrapper.eq(Task::getStatus, status);
        }
        Page<Task> pageResult = taskMapper.selectPage(new Page<>(page, size), wrapper);
        Page<TaskVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        voPage.setRecords(pageResult.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    private TaskVO toVO(Task task) {
        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
