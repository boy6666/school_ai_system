package com.eduagent.teacher.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 学习进度事件消费者（来自 edu-agent-learning）。
 * 更新看板进程内缓存，教师看板近实时（生产可平替为 Redis）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyProgressConsumer {

    private final DashboardCache dashboardCache;

    @RabbitListener(queues = "teacher.study.progress.queue")
    public void onStudyProgress(StudyProgressEvent e) {
        if (e.getClassId() == null || e.getStudentId() == null) {
            log.warn("[StudyProgress] 事件缺 classId/studentId，忽略: {}", e);
            return;
        }
        dashboardCache.put(e.getClassId(), e);
        log.debug("[StudyProgress] 缓存更新 classId={} studentId={}",
                e.getClassId(), e.getStudentId());
    }
}
