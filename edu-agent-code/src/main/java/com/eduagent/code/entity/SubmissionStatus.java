package com.eduagent.code.entity;

/**
 * 提交判分状态机（code_submissions.status，契约 C1 result 的 status 字段同此）。
 */
public final class SubmissionStatus {

    private SubmissionStatus() {
    }

    /** 待处理 */
    public static final int PENDING = 0;
    /** 运行中 */
    public static final int RUNNING = 1;
    /** 已完成（判分产出报告） */
    public static final int DONE = 2;
    /** 超时强杀 */
    public static final int TIMEOUT = 3;
    /** 编译失败（不进沙箱，直接结束） */
    public static final int COMPILE_ERROR = 4;
    /** 判分过程异常 */
    public static final int FAILED = 5;
}
