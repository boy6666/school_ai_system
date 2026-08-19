package com.eduagent.code.service.checker;

/**
 * Checkstyle 违规（dev-wuyoucheng §2.4.3 错误模型）。
 */
public record CheckstyleViolation(
        String file, int line, int column,
        String severity, String message, String source, String rule) {
}
