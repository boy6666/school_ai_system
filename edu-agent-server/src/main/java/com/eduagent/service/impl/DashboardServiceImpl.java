package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.entity.Task;
import com.eduagent.mapper.TaskMapper;
import com.eduagent.service.DashboardService;
import com.eduagent.vo.DashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {
    @Autowired
    private TaskMapper taskMapper;

    @Override
    public DashboardVO getDashboard(Long studentId) {
        DashboardVO vo = new DashboardVO();

        // 今日任务：只显示名称，不显示时长
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStudentId, studentId)
                        .ne(Task::getStatus, 2)
                        .orderByAsc(Task::getDueDate)
        );
        List<DashboardVO.TodayTask> todayTasks = tasks.stream().limit(3).map(task -> {
            DashboardVO.TodayTask t = new DashboardVO.TodayTask();
            t.setName(task.getTitle());
            t.setDuration(0); // 不显示时长
            return t;
        }).collect(Collectors.toList());
        if (todayTasks.isEmpty()) {
            DashboardVO.TodayTask defaultTask = new DashboardVO.TodayTask();
            defaultTask.setName("暂无今日任务，点击「学习任务」添加");
            defaultTask.setDuration(0);
            todayTasks = Arrays.asList(defaultTask);
        }
        vo.setTodayTasks(todayTasks);

        // 其他模块：返回空数据但保留结构（前端会显示“暂无数据”）
        vo.setReviewSubjects(new ArrayList<>());
        vo.setLearningProgress(new ArrayList<>());
        vo.setGoal(new DashboardVO.Goal());
        vo.setSummary(new DashboardVO.Summary());
        vo.setEvaluation(new DashboardVO.Evaluation());
        vo.setRhythm(new DashboardVO.Rhythm());
        vo.setPlan(new DashboardVO.Plan());

        return vo;
    }
}
