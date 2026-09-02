package com.eduagent.code.event;

import com.eduagent.common.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 判分完成事件，exchange 名 = 事件名 {@code assignment.graded}（C12），payload 携带完整报告体（C1）。
 * teacher-service 消费后按 (assignmentId, studentId, assignmentItemId) 幂等回填 grades；teacher 不轮询（方案 A）。
 * {@code checkstyle}/{@code pmd} 为结构化对象（{errorCount, warningCount, violations[]} / {violationCount, violations[]}），
 * 序列化由 JSON 承载。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssignmentGradedEvent extends BaseEvent {

    private Long assignmentId;
    private Long assignmentItemId;
    private Long studentId;
    private Long submissionId;

    /** 判分终态：done / timeout / compile_error / failed */
    private String status;

    private Boolean runPassed;
    private Integer compileOk;
    private String stdout;
    private Long runTimeMs;

    private Object checkstyle;
    private Object pmd;
    private String aiSuggestion;
    private Integer overallScore;
}
