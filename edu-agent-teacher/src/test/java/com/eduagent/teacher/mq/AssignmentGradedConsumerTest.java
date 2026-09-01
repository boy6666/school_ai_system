package com.eduagent.teacher.mq;

import com.eduagent.teacher.entity.Grade;
import com.eduagent.teacher.mapper.GradeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 代码判分异步回填单测（§B.5 + C1 方案 A）：
 * - 消费 assignment.graded，按 uk_stu_item 定位 grade，回填三份报告 + score，置 status=1
 * - 天然幂等：找不到成绩时丢弃不写
 */
@ExtendWith(MockitoExtension.class)
class AssignmentGradedConsumerTest {

    @Mock GradeMapper gradeMapper;

    ObjectMapper objectMapper = new ObjectMapper();

    AssignmentGradedConsumer consumer;

    Grade grade = new Grade();

    @DisplayName("能回填 report JSON 并置 status=1")
    @Test
    void onAssignmentGraded_backfillsReports() throws Exception {
        consumer = new AssignmentGradedConsumer(gradeMapper, objectMapper);
        grade.setId(9L);
        when(gradeMapper.selectByStuItem(1L, 2L, 3L)).thenReturn(grade);

        AssignmentGradedEvent e = new AssignmentGradedEvent();
        e.setAssignmentId(1L);
        e.setAssignmentItemId(3L);
        e.setStudentId(2L);
        e.setStdout("Hello");
        e.setRunTimeMs(42L);
        e.setRunPassed(true);
        e.setStatus("SUCCESS");
        e.setCompileOk(true);
        e.setCheckstyle("ok");
        e.setPmd("minor");
        e.setAiSuggestion("保持");
        e.setOverallScore(98);

        consumer.onAssignmentGraded(e);

        JsonNode run = objectMapper.readTree(grade.getRunResult());
        assertThat(run.get("runPassed").asBoolean()).isTrue();
        assertThat(run.get("runTimeMs").asLong()).isEqualTo(42);
        JsonNode st = objectMapper.readTree(grade.getStaticReport());
        assertThat(st.get("compileOk").asBoolean()).isTrue();
        JsonNode ai = objectMapper.readTree(grade.getAiReport());
        assertThat(ai.get("aiSuggestion").asText()).isEqualTo("保持");
        assertThat(grade.getScore()).isEqualTo(98);
        assertThat(grade.getStatus()).isEqualTo(1);
        assertThat(grade.getGradedAt()).isNotNull();
        verify(gradeMapper).updateById((Grade) grade);
    }

    @DisplayName("找不到成绩时丢弃，不写库")
    @Test
    void onAssignmentGraded_unmatched_isDropped() {
        consumer = new AssignmentGradedConsumer(gradeMapper, objectMapper);
        when(gradeMapper.selectByStuItem(1L, 2L, 3L)).thenReturn(null);

        AssignmentGradedEvent e = new AssignmentGradedEvent();
        e.setAssignmentId(1L);
        e.setAssignmentItemId(3L);
        e.setStudentId(2L);
        e.setRunPassed(true);
        e.setCompileOk(true);
        e.setOverallScore(90);

        consumer.onAssignmentGraded(e);

        verify(gradeMapper, never()).updateById(any(Grade.class));
    }

    @DisplayName("事件回填时写入 submissionId（教师重判入口数据源）")
    @Test
    void onAssignmentGraded_backfillsSubmissionId() {
        consumer = new AssignmentGradedConsumer(gradeMapper, objectMapper);
        grade.setId(9L);
        when(gradeMapper.selectByStuItem(1L, 2L, 3L)).thenReturn(grade);

        AssignmentGradedEvent e = new AssignmentGradedEvent();
        e.setAssignmentId(1L);
        e.setAssignmentItemId(3L);
        e.setStudentId(2L);
        e.setSubmissionId(1024L);
        e.setStdout("Hello");
        e.setRunTimeMs(10L);
        e.setRunPassed(true);
        e.setStatus("done");
        e.setCompileOk(true);
        e.setCheckstyle("ok");
        e.setPmd("minor");
        e.setAiSuggestion("可以");
        e.setOverallScore(88);

        consumer.onAssignmentGraded(e);

        assertThat(grade.getSubmissionId()).isEqualTo(1024L);
        assertThat(grade.getStatus()).isEqualTo(1);
    }
}
