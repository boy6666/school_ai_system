package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.entity.Task;
import com.eduagent.mapper.TaskMapper;
import com.eduagent.service.LearningPathService;
import com.eduagent.vo.LearningPathVO;
import com.eduagent.vo.StageVO;
import com.eduagent.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningPathServiceImpl implements LearningPathService {
    @Autowired
    private TaskMapper taskMapper;

    @Override
    public LearningPathVO getCurrentPath(Long studentId) {
        // 获取该学生的所有任务
        List<Task> allTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>().eq(Task::getStudentId, studentId));
        List<Task> pendingTasks = allTasks.stream().filter(t -> t.getStatus() != 2).collect(Collectors.toList());
        int completedCount = (int) allTasks.stream().filter(t -> t.getStatus() == 2).count();
        int totalTasks = allTasks.size();
        int completionRate = totalTasks == 0 ? 0 : (completedCount * 100 / totalTasks);

        // 动态生成路径
        List<StageVO> stages = new ArrayList<>();

        // 阶段1：基础任务（如果有未完成的基础任务则显示，否则默认创建）
        StageVO stage1 = new StageVO();
        stage1.setName("基础巩固");
        List<TaskVO> tasks1;
        if (pendingTasks.stream().anyMatch(t -> t.getTitle().contains("基础") || t.getTitle().contains("入门"))) {
            tasks1 = pendingTasks.stream()
                .filter(t -> t.getTitle().contains("基础") || t.getTitle().contains("入门"))
                .limit(3)
                .map(this::convertToTaskVO)
                .collect(Collectors.toList());
        } else {
            tasks1 = Arrays.asList(
                createTaskVO("机器学习基础概念回顾", 30, 0, 0),
                createTaskVO("线性回归与损失函数", 45, 0, 0),
                createTaskVO("梯度下降原理与实现", 45, 0, 0)
            );
        }
        stage1.setTasks(tasks1);
        stages.add(stage1);

        // 阶段2：核心任务
        StageVO stage2 = new StageVO();
        stage2.setName("核心突破");
        List<TaskVO> tasks2;
        if (pendingTasks.stream().anyMatch(t -> t.getTitle().contains("反向") || t.getTitle().contains("优化"))) {
            tasks2 = pendingTasks.stream()
                .filter(t -> t.getTitle().contains("反向") || t.getTitle().contains("优化"))
                .limit(2)
                .map(this::convertToTaskVO)
                .collect(Collectors.toList());
        } else {
            tasks2 = Arrays.asList(
                createTaskVO("反向传播算法详解", 60, 1, 55),
                createTaskVO("激活函数与优化器对比", 45, 0, 0)
            );
        }
        stage2.setTasks(tasks2);
        stages.add(stage2);

        // 阶段3：综合实战
        StageVO stage3 = new StageVO();
        stage3.setName("综合实战");
        List<TaskVO> tasks3 = Arrays.asList(
            createTaskVO("使用PyTorch搭建MLP模型", 90, 0, 0),
            createTaskVO("训练模型并可视化损失曲线", 90, 0, 0),
            createTaskVO("模型评估与部署", 60, 0, 0)
        );
        stage3.setTasks(tasks3);
        stages.add(stage3);

        // 计算总任务数和已完成数
        int totalTasksCount = stages.stream().mapToInt(s -> s.getTasks().size()).sum();
        int completedTasksCount = stages.stream().mapToInt(s -> (int) s.getTasks().stream().filter(t -> t.getStatus() == 2).count()).sum();

        // 预计完成时间基于完成率动态调整
        LocalDate estimatedDate = LocalDate.now().plusDays(30 - completionRate / 5);
        String targetMastery = completionRate >= 50 ? "≥90%" : "≥85%";

        LearningPathVO vo = new LearningPathVO();
        vo.setGoal("掌握神经网络基础原理并完成模型实践");
        vo.setTargetMastery(targetMastery);
        vo.setEstimatedCompletion(estimatedDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        vo.setTotalHours((int) Math.round(totalTasks * 2.5));
        vo.setTotalTasks(totalTasksCount);
        vo.setCompletedTasks(completedTasksCount);
        vo.setStages(stages);
        return vo;
    }

    private TaskVO convertToTaskVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setDuration(45); // 可预设或从 task 中获取
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getStatus() == 2 ? 100 : 0);
        return vo;
    }

    private TaskVO createTaskVO(String title, int duration, int status, int progress) {
        TaskVO vo = new TaskVO();
        vo.setTitle(title);
        vo.setDuration(duration);
        vo.setStatus(status);
        vo.setProgress(progress);
        return vo;
    }
}
