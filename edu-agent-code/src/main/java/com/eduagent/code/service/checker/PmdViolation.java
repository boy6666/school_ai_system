package com.eduagent.code.service.checker;

/**
 * PMD 违规（dev-wuyoucheng §2.4.3 错误模型）。
 */
public record PmdViolation(
        String file, int line, String ruleSet,
        String rule, int priority, String message, String description) {
}
