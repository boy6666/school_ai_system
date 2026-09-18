package com.eduagent.learning.mq;

import com.eduagent.common.constant.ServiceConstants;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * study.progress 事件发布器（exchange 名 = 事件名，决议 C12）。
 * 发布失败不阻断任务完成主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyProgressPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final StudentProfileMapper profileMapper;
    private final ObjectMapper objectMapper;

    public void publish(Long studentId, Integer progress, Integer completedTasks, Integer totalTasks) {
        try {
            StudyProgressEvent event = new StudyProgressEvent();
            event.setStudentId(studentId);
            event.setProgress(progress);
            event.setCompletedTasks(completedTasks);
            event.setTotalTasks(totalTasks);
            event.setMasteryRate(resolveMasteryRate(studentId));
            event.setEventTime(java.time.LocalDateTime.now());
            StudentProfile sp = profileMapper.findByStudentId(studentId);
            event.setClassId(sp != null ? sp.getClassId() : null);

            rabbitTemplate.convertAndSend(ServiceConstants.EVENT_STUDY_PROGRESS,
                    ServiceConstants.EVENT_STUDY_PROGRESS, event);
            log.info("[MQ] study.progress 已发布: studentId={}, progress={}%", studentId, progress);
        } catch (Exception e) {
            log.error("[MQ] study.progress 发布失败: {}", e.getMessage());
        }
    }

    private Integer resolveMasteryRate(Long studentId) {
        try {
            StudentProfile sp = profileMapper.findByStudentId(studentId);
            if (sp == null || sp.getProfileData() == null) {
                return null;
            }
            JsonNode dims = objectMapper.readTree(sp.getProfileData());
            JsonNode overall = dims.get("overall_level");
            if (overall != null && overall.has("score")) {
                return overall.get("score").asInt();
            }
            int sum = 0, n = 0;
            for (String key : List.of("knowledge_mastery", "learning_goal_clarity", "cognitive_adaptation",
                    "mistake_avoidance", "learning_autonomy")) {
                JsonNode d = dims.get(key);
                if (d != null && d.has("score")) {
                    sum += d.get("score").asInt();
                    n++;
                }
            }
            return n == 0 ? null : sum / n;
        } catch (Exception e) {
            return null;
        }
    }
}
