package com.eduagent.code.dto;

/**
 * 沙箱运行结果（真实执行的裁决依据，AI 只反馈不改判）。
 * <ul>
 *   <li>{@code timedOut}：被超时强杀（不产生 exitCode）</li>
 *   <li>{@code completed}：进程正常启动并自行退出（exitCode 有效，含非 0 崩溃退出）</li>
 *   <li>{@code error != null}：基础设施失败（无法启动进程/容器等），判分直接 FAILED</li>
 * </ul>
 */
public record RunResult(
        boolean timedOut,
        boolean completed,
        int exitCode,
        String stdout,
        String stderr,
        long runTimeMs,
        String error) {

    public static RunResult ok(String stdout, String stderr, long runTimeMs) {
        return new RunResult(false, true, 0, stdout, stderr, runTimeMs, null);
    }

    public static RunResult crashed(int exitCode, String stdout, String stderr, long runTimeMs) {
        return new RunResult(false, true, exitCode, stdout, stderr, runTimeMs, null);
    }

    public static RunResult timedOut(String stdout, String stderr, long runTimeMs) {
        return new RunResult(true, false, -1, stdout, stderr, runTimeMs, null);
    }

    public static RunResult error(String error) {
        return new RunResult(false, false, -1, "", "", 0, error);
    }

    /** 判定「运行通过」：自行退出且退出码 0。崩溃/超时均不计运行分。 */
    public boolean runPassed() {
        return completed && exitCode == 0;
    }
}
