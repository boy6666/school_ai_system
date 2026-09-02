package com.eduagent.code.service.worker;

import com.eduagent.code.client.AiFeedbackService;
import com.eduagent.code.entity.CodeCheckReport;
import com.eduagent.code.entity.CodeSubmission;
import com.eduagent.code.entity.SubmissionStatus;
import com.eduagent.code.event.AssignmentGradedEvent;
import com.eduagent.code.mapper.CodeSubmissionMapper;
import com.eduagent.code.mq.AssignmentGradedProducer;
import com.eduagent.code.service.CodeCheckReportService;
import com.eduagent.code.service.checker.StaticCheckService;
import com.eduagent.code.service.compiler.JavaCompileService;
import com.eduagent.code.service.runner.LocalJavaRunner;
import com.eduagent.code.service.score.ScoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 判分流水线集成单测：真实编译 + 静态检查 + 本地进程运行，Mock 掉 mapper/report/AI/事件，
 * 覆盖通过 / 编译失败 / 超时强杀 / 运行崩溃 / 检查扣分 / 完整事件体 六大场景。
 * （沙箱走 LocalJavaRunner，不依赖 Docker；超时用 ReflectionTestUtils 收紧限时。）
 */
class JudgeWorkerTest {

    private CodeSubmissionMapper submissionMapper;
    private CodeCheckReportService reportService;
    private AssignmentGradedProducer producer;
    private JudgeWorker worker;

    @BeforeEach
    void setUp() {
        submissionMapper = mock(CodeSubmissionMapper.class);
        reportService = mock(CodeCheckReportService.class);
        producer = mock(AssignmentGradedProducer.class);
        AiFeedbackService aiFeedbackService = mock(AiFeedbackService.class);
        when(aiFeedbackService.requestSuggestion(anyUse())).thenReturn("AI 反馈：整体良好");

        worker = new JudgeWorker(
                submissionMapper,
                reportService,
                new JavaCompileService(),
                new StaticCheckService(new ObjectMapper()),
                new LocalJavaRunner(),
                new ScoreService(),
                aiFeedbackService,
                producer,
                new ObjectMapper());
        ReflectionTestUtils.setField(worker, "timeoutMs", 5_000L);
    }

    private static com.eduagent.code.client.CodeAnalyzeRequest anyUse() {
        return org.mockito.ArgumentMatchers.any();
    }

    private CodeSubmission submission(long id, String source, String expected, String className) {
        CodeSubmission sub = new CodeSubmission();
        sub.setId(id);
        sub.setStudentId(1001L);
        sub.setAssignmentId(5L);
        sub.setAssignmentItemId(12L);
        sub.setLanguage("java");
        sub.setClassName(className == null ? "Main" : className);
        sub.setExpectedOutput(expected);
        sub.setSourceCode(source);
        sub.setStatus(SubmissionStatus.PENDING);
        when(submissionMapper.selectById(id)).thenReturn(sub);
        return sub;
    }

    @Test
    void passingSubmissionScoresAndPublishesCompleteEvent() {
        CodeSubmission sub = submission(1L,
                "public class Main { public static void main(String[] a) { System.out.println(\"hi\"); } }",
                "hi", null);

        worker.judge(1L);

        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.DONE);
        assertThat(sub.getStdout().strip()).isEqualTo("hi");

        ArgumentCaptor<CodeCheckReport> report = ArgumentCaptor.forClass(CodeCheckReport.class);
        verify(reportService).save(report.capture());
        assertThat(report.getValue().getCompileOk()).isEqualTo(1);
        assertThat(report.getValue().getOverallScore()).isEqualTo(100);
        assertThat(report.getValue().getCheckstyle()).isNotBlank();
        assertThat(report.getValue().getScoreDetail()).contains("\"total\":100");

        ArgumentCaptor<AssignmentGradedEvent> event = ArgumentCaptor.forClass(AssignmentGradedEvent.class);
        verify(producer).publish(event.capture());
        AssignmentGradedEvent e = event.getValue();
        assertThat(e.getSubmissionId()).isEqualTo(1L);
        assertThat(e.getAssignmentId()).isEqualTo(5L);
        assertThat(e.getAssignmentItemId()).isEqualTo(12L);
        assertThat(e.getStudentId()).isEqualTo(1001L);
        assertThat(e.getStatus()).isEqualTo("done");
        assertThat(e.getRunPassed()).isTrue();
        assertThat(e.getCompileOk()).isEqualTo(1);
        assertThat(e.getOverallScore()).isEqualTo(100);
        assertThat(e.getCheckstyle()).isNotNull();
        assertThat(e.getPmd()).isNotNull();
        assertThat(e.getAiSuggestion()).contains("AI 反馈");
    }

    @Test
    void compileErrorTerminatesWithReportAndEvent() {
        CodeSubmission sub = submission(2L,
                "public class Main { public static void main(String[] a {", "hi", null);

        worker.judge(2L);

        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.COMPILE_ERROR);

        ArgumentCaptor<CodeCheckReport> report = ArgumentCaptor.forClass(CodeCheckReport.class);
        verify(reportService).save(report.capture());
        assertThat(report.getValue().getCompileOk()).isZero();
        assertThat(report.getValue().getOverallScore()).isZero();
        assertThat(report.getValue().getCompileMsg()).isNotBlank();

        ArgumentCaptor<AssignmentGradedEvent> event = ArgumentCaptor.forClass(AssignmentGradedEvent.class);
        verify(producer).publish(event.capture());
        assertThat(event.getValue().getStatus()).isEqualTo("compile_error");
        assertThat(event.getValue().getCompileOk()).isZero();
        assertThat(event.getValue().getRunPassed()).isFalse();
    }

    @Test
    void timeoutKillsAndMarksTimedOut() {
        // 用字符串解析出 sleep 时长，避免数字字面量触发 MagicNumber 扣分干扰分数断言
        CodeSubmission sub = submission(3L,
                "public class Main { public static void main(String[] a) throws Exception { " +
                        "Thread.sleep(Long.parseLong(\"60000\")); } }",
                "hi", null);
        ReflectionTestUtils.setField(worker, "timeoutMs", 500L);

        worker.judge(3L);

        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.TIMEOUT);
        assertThat(sub.getRunTimeMs()).isLessThan(5_000);

        ArgumentCaptor<CodeCheckReport> report = ArgumentCaptor.forClass(CodeCheckReport.class);
        verify(reportService).save(report.capture());
        // 编译40分，未运行完成不加运行分
        assertThat(report.getValue().getOverallScore()).isEqualTo(40);

        ArgumentCaptor<AssignmentGradedEvent> event = ArgumentCaptor.forClass(AssignmentGradedEvent.class);
        verify(producer).publish(event.capture());
        assertThat(event.getValue().getStatus()).isEqualTo("timeout");
    }

    @Test
    void crashingProgramNoRunPoints() {
        CodeSubmission sub = submission(4L,
                "public class Main { public static void main(String[] a) { System.out.println(\"boom\"); System.exit(1); } }",
                "boom", null);

        worker.judge(4L);

        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.DONE);
        ArgumentCaptor<CodeCheckReport> report = ArgumentCaptor.forClass(CodeCheckReport.class);
        verify(reportService).save(report.capture());
        // 编译40分；exit 3 → 运行未通过 → 无运行/输出分
        assertThat(report.getValue().getOverallScore()).isEqualTo(40);
    }

    @Test
    void outputMismatchLosesOutputPoints() {
        CodeSubmission sub = submission(5L,
                "public class Main { public static void main(String[] a) { System.out.println(\"HELLO\"); } }",
                "hi", null);

        worker.judge(5L);

        assertThat(sub.getStatus()).isEqualTo(SubmissionStatus.DONE);
        ArgumentCaptor<CodeCheckReport> report = ArgumentCaptor.forClass(CodeCheckReport.class);
        verify(reportService).save(report.capture());
        // 编译40 + 运行40 = 80，输出不匹配无 +20
        assertThat(report.getValue().getOverallScore()).isEqualTo(80);
    }

    @Test
    void checkstylePenaltyDeductsScore() {
        CodeSubmission sub = submission(6L,
                "public class Main { public static void main(String[] a) { int magic = 42; System.out.println(\"hi\"); } }",
                "hi", null);

        worker.judge(6L);

        ArgumentCaptor<CodeCheckReport> report = ArgumentCaptor.forClass(CodeCheckReport.class);
        verify(reportService).save(report.capture());
        // MagicNumber 触发 warning(-1)：100-1=99；断言不足满分即可（规则数受配置影响）
        assertThat(report.getValue().getOverallScore()).isLessThan(100);
    }

    @Test
    void missingSubmissionIsSkipped() {
        when(submissionMapper.selectById(anyLong())).thenReturn(null);
        worker.judge(999L);
        verify(reportService, never()).save(org.mockito.ArgumentMatchers.any());
        verify(producer, never()).publish(org.mockito.ArgumentMatchers.any());
    }
}
