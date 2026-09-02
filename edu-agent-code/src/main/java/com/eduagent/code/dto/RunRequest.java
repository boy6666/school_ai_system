package com.eduagent.code.dto;

import java.util.Map;

/**
 * 沙箱运行请求：入口类名 + 已编译字节码（全限定类名→class 字节码，含 $ 内部类）。
 * {@code stdin} 预留（后续竞赛隐藏用例可送输入），v1 判分为标准输出比较，通常为 null。
 */
public record RunRequest(
        String className,
        Map<String, byte[]> classes,
        String stdin,
        long timeoutMs,
        long maxMemoryMb) {

    public static RunRequest of(String className, Map<String, byte[]> classes, long timeoutMs, long maxMemoryMb) {
        return new RunRequest(className, classes, null, timeoutMs, maxMemoryMb);
    }
}
