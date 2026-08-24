package com.eduagent.code.service.worker;

import com.eduagent.code.client.AiFeedbackService;
import com.eduagent.code.client.CodeAnalyzeRequest;
import com.eduagent.code.dto.RunRequest;
import com.eduagent.code.dto.RunResult;
import com.eduagent.code.entity.CodeCheckReport;
import com.eduagent.code.entity.CodeSubmission;
import com.eduagent.code.entity.SubmissionStatus;
import com.eduagent.code.event.AssignmentGradedEvent;
import com.eduagent.code.mapper.CodeSubmissionMapper;
import com.eduagent.code.mq.AssignmentGradedProducer;
import com.eduagent.code.service.CodeCheckReportService;
import com.eduagent.code.service.checker.StaticCheckResult;
import com.eduagent.code.service.checker.StaticCheckService;
import com.eduagent.code.service.compiler.CompileResult;
import com.eduagent.code.service.compiler.JavaCompileService;
import com.eduagent.code.service.compiler.SourceFile;
import com.eduagent.code.service.compiler.SourceSplitter;
import com.eduagent.code.service.runner.SandboxRunner;
import com.eduagent.code.service.score.ScoreResult;
import com.eduagent.code.service.score.ScoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 判分编排（Task #4 核心，方案 a：@Async + 启动补偿）。流水线：
 * <pre>
 *  切分多文件 → 编译 → Checkstyle/PMD → Docker/本地沙箱真实运行
 *      → AI 纠错(参考，容错) → ScoreService 权重判分 → 落库报告/更新状态 → 发 assignment.graded
 * </pre>
 * 判分终态由「真实执行」裁决（换成任何其它技术都可回退此编排）。状态机见 {@link SubmissionStatus}：
 * 2=DONE 3=TIMEOUT 4=COMPILE_ERROR 5=FAILED；PENDING/RUNNING 由启动补偿 {@link JudgeRecoveryRunner} 接管。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeWorker {

    private static final int MAX_STDOUT = 100_000;
    private static final int MAX_STDERR = 50_000;
    private static final int MAX_TEXT = 2000;

    private final CodeSubmissionMapper submissionMapper;
    private final CodeCheckReportService reportService;
    private final JavaCompileService compileService;
    private final StaticCheckService staticCheckService;
    private final SandboxRunner sandboxRunner;
    private final ScoreService scoreService;
    private final AiFeedbackService aiFeedbackService;
    private final AssignmentGradedProducer producer;
    private final ObjectMapper objectMapper;

    /** 判分限时（教学档 5-10s，竞赛档另配覆盖） */
    @Value("${edu-agent.judge.timeout-ms:10000}")
    private long timeoutMs = 10_000;

    /** 沙箱内存上限（教学档 256MB，竞赛档另配） */
    @Value("${edu-agent.judge.max-memory-mb:256}")
    private long maxMemoryMb = 256;

    @Async("judgeExecutor")
    public void judge(Long submissionId) {
        try {
            doJudge(submissionId);
        } catch (Exception e) {
            log.error("判分异常 submissionId={}", submissionId, e);
            markFailed(submissionId);
        }
    }

    private void doJudge(Long submissionId) throws Exception {
        CodeSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) {
            log.warn("判分跳过：submission 不存在 id={}", submissionId);
            return;
        }
        sub.setStatus(SubmissionStatus.RUNNING);
        submissionMapper.updateById(sub);

        List<SourceFile> sources = SourceSplitter.split(sub.getSourceCode(), sub.getClassName());
        CompileResult compile = compileService.compile(sources);

        CodeCheckReport report = baseReport(sub.getId(), compile);
        StaticCheckResult check = null;
        if (compile.ok()) {
            check = staticCheckService.check(sources);
        }

        RunResult run = null;
        if (compile.ok()) {
            run = sandboxRunner.run(RunRequest.of(sub.getClassName(), compile.classes(), timeoutMs, maxMemoryMb));
        }

        boolean runPassed = run != null && run.runPassed();
        boolean outputMatched = runPassed && matchesExpected(sub.getExpectedOutput(), run.stdout());

        int csErr = check == null ? 0 : check.checkstyleErrorCount();
        int csWarn = check == null ? 0 : check.checkstyleWarningCount();
        int pmd = check == null ? 0 : check.totalPmd();

        ScoreResult score = scoreService.score(compile.ok(), csErr, csWarn, pmd, runPassed, outputMatched);

        String ai = "";
        if (compile.ok()) {
            ai = aiFeedbackService.requestSuggestion(buildAiRequest(sub, compile.ok(), csErr, pmd, runPassed, run));
        }

        report.setCheckstyle(check == null ? "[]" : check.checkstyleJson());
        report.setPmd(check == null ? "[]" : check.pmdJson());
        report.setAiSuggestion(truncate(ai, MAX_TEXT));
        report.setOverallScore(score.score());
        report.setScoreDetail(toJson(score.detail()));

        int status = statusOf(compile.ok(), run);
        sub.setStatus(status);
        sub.setStdout(run == null ? "" : truncate(run.stdout(), MAX_STDOUT));
        sub.setStderr(run == null ? "" : truncate(run.stderr(), MAX_STDERR));
        sub.setRunTimeMs(run == null ? 0 : (int) run.runTimeMs());
        submissionMapper.updateById(sub);

        reportService.save(report);

        producer.publish(buildEvent(sub, status, check, run, runPassed, outputMatched, ai, score));
        log.info("判分完成 submissionId={} status={} score={}", submissionId, status, score.score());
    }

    /** 编译失败立即产出零分报告与事件，状态 4 直接结束（不进沙箱，§2.4.6） */
    private CodeCheckReport baseReport(Long submissionId, CompileResult compile) {
        CodeCheckReport report = new CodeCheckReport();
        report.setSubmissionId(submissionId);
        report.setCompileOk(compile.ok() ? 1 : 0);
        report.setCompileMsg(truncate(compile.error(), 2000));
        return report;
    }

    private int statusOf(boolean compileOk, RunResult run) {
        if (!compileOk) {
            return SubmissionStatus.COMPILE_ERROR;
        }
        if (run != null && run.error() != null) {
            return SubmissionStatus.FAILED;
        }
        if (run != null && run.timedOut()) {
            return SubmissionStatus.TIMEOUT;
        }
        return SubmissionStatus.DONE;
    }

    private boolean matchesExpected(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return expected.strip().equals(actual == null ? "" : actual.strip());
    }

    /** AI 只拿判分事实做参考反馈（§1.3.4），不改判 */
    private CodeAnalyzeRequest buildAiRequest(CodeSubmission sub, boolean compileOk,
                                              int csErr, int pmd, boolean runPassed, RunResult run) {
        CodeAnalyzeRequest req = new CodeAnalyzeRequest();
        req.setLanguage(sub.getLanguage() == null ? "java" : sub.getLanguage());
        req.setSourceCode(sub.getSourceCode());
        CodeAnalyzeRequest.Ctx ctx = new CodeAnalyzeRequest.Ctx();
        ctx.setAssignmentItemId(sub.getAssignmentItemId());
        ctx.setStudentId(sub.getStudentId() == null ? null : String.valueOf(sub.getStudentId()));
        ctx.setCompileOk(compileOk ? 1 : 0);
        ctx.setCheckstyleErrors(csErr);
        ctx.setPmdViolations(pmd);
        ctx.setRunPassed(runPassed ? 1 : 0);
        ctx.setRunStdout(run == null ? null : run.stdout());
        ctx.setExpectedOutput(sub.getExpectedOutput());
        req.setContext(ctx);
        return req;
    }

    /** 事件体须携带完整报告体（C1），teacher 消费后直接回填 grades，不轮询 */
    private AssignmentGradedEvent buildEvent(CodeSubmission sub, int status,
                                             StaticCheckResult check, RunResult run,
                                             boolean runPassed, boolean outputMatched,
                                             String ai, ScoreResult score) {
        AssignmentGradedEvent event = new AssignmentGradedEvent();
        event.setAssignmentId(sub.getAssignmentId());
        event.setAssignmentItemId(sub.getAssignmentItemId());
        event.setStudentId(sub.getStudentId());
        event.setSubmissionId(sub.getId());
        event.setStatus(statusName(status));
        event.setRunPassed(runPassed);
        event.setCompileOk(status == SubmissionStatus.COMPILE_ERROR ? 0 : 1);
        event.setStdout(run == null ? "" : run.stdout());
        event.setRunTimeMs(run == null ? 0L : run.runTimeMs());
        event.setCheckstyle(check == null
                ? Map.of("errorCount", 0, "warningCount", 0, "violations", List.of())
                : Map.of("errorCount", check.checkstyleErrorCount(),
                        "warningCount", check.checkstyleWarningCount(),
                        "violations", parseList(check.checkstyleJson())));
        event.setPmd(check == null
                ? Map.of("violationCount", 0, "violations", List.of())
                : Map.of("violationCount", check.totalPmd(), "violations", parseList(check.pmdJson())));
        event.setAiSuggestion(ai);
        event.setOverallScore(score.score());
        return event;
    }

    private String statusName(int status) {
        return switch (status) {
            case SubmissionStatus.DONE -> "done";
            case SubmissionStatus.TIMEOUT -> "timeout";
            case SubmissionStatus.COMPILE_ERROR -> "compile_error";
            case SubmissionStatus.FAILED -> "failed";
            default -> "unknown";
        };
    }

    private void markFailed(Long submissionId) {
        try {
            CodeSubmission sub = submissionMapper.selectById(submissionId);
            if (sub != null) {
                sub.setStatus(SubmissionStatus.FAILED);
                submissionMapper.updateById(sub);
            }
        } catch (Exception e) {
            log.warn("判分失败态落库异常 submissionId={}", submissionId, e);
        }
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<?> parseList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
