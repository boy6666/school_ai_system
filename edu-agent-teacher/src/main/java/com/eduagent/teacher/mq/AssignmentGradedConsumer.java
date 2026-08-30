package com.eduagent.teacher.mq;

import com.eduagent.teacher.entity.Grade;
import com.eduagent.teacher.mapper.GradeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 代码判分完成事件消费者（方案 A，不轮询）。
 * 消费 assignment.graded（来自 edu-agent-code），payload 携带完整报告体，
 * 按 uk_stu_item 定位 grade 并回填 run_result/static_report/ai_report/score/submission_id，status→1。
 * 天然幂等：事件重投仍按同一 key 定位覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentGradedConsumer {

    private final GradeMapper gradeMapper;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "teacher.assignment.graded.queue")
    public void onAssignmentGraded(AssignmentGradedEvent e) {
        Grade grade = gradeMapper.selectByStuItem(e.getAssignmentId(), e.getStudentId(), e.getAssignmentItemId());
        if (grade == null) {
            log.warn("[AssignmentGraded] 未找到对应成绩，丢弃。assignmentId={} itemId={} studentId={}",
                    e.getAssignmentId(), e.getAssignmentItemId(), e.getStudentId());
            return;
        }
        grade.setRunResult(writeJson(Map.of(
                "stdout", e.getStdout(),
                "runTimeMs", e.getRunTimeMs(),
                "status", e.getStatus(),
                "runPassed", e.getRunPassed())));
        grade.setStaticReport(writeJson(Map.of(
                "compileOk", e.getCompileOk(),
                "checkstyle", e.getCheckstyle(),
                "pmd", e.getPmd())));
        grade.setAiReport(writeJson(Map.of("aiSuggestion", e.getAiSuggestion())));
        if (e.getSubmissionId() != null) {
            grade.setSubmissionId(e.getSubmissionId());
        }
        if (e.getOverallScore() != null) {
            grade.setScore(e.getOverallScore());
        }
        grade.setStatus(1);
        grade.setGradedAt(LocalDateTime.now());
        gradeMapper.updateById(grade);
        log.info("[AssignmentGraded] 回填完成 itemId={} studentId={} score={}",
                e.getAssignmentItemId(), e.getStudentId(), grade.getScore());
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception ex) {
            return null;
        }
    }
}
