package com.eduagent.code.service.runner;

import com.eduagent.code.dto.RunRequest;
import com.eduagent.code.dto.RunResult;

/**
 * 沙箱运行抽象。v1 提供两个实现：
 * <ul>
 *   <li>{@link LocalJavaRunner}：本机 {@code java} 子进程，默认（可离线单测/开发）</li>
 *   <li>{@link DockerSandboxRunner}：openjdk 容器（生产安全红线 §2.4.4，属性切到 docker）</li>
 * </ul>
 */
public interface SandboxRunner {

    RunResult run(RunRequest request);
}
