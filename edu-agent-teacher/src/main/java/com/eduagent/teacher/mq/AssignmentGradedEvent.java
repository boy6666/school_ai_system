package com.eduagent.teacher.mq;

import com.eduagent.common.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码判分完成事件（来自 edu-agent-code，exchange=routingKey=assignment.graded）。
 * 方案 A（异步两段式）：payload 携带完整报告体（不只得分），
 * 由 AssignmentGradedConsumer 回填 grades 的 run_result/static_report/ai_report/score。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssignmentGradedEvent extends BaseEvent {

    public static final String TOPIC = "assignment.graded";

    private Long assignmentId;
    private Long assignmentItemId;
    private Long studentId;
    private Long submissionId;

    /** pending / running / done / failed */
    private String status;

    private Boolean runPassed;
    private Boolean compileOk;
    private String stdout;
    private Long runTimeMs;
    private String checkstyle;
    private String pmd;
    private String aiSuggestion;
    private Integer overallScore;
}
