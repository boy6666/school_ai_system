package com.eduagent.code.service.checker;

import java.util.List;

/**
 * 静态检查结果：违规列表 + 供落库的 JSON 文本（code_check_reports.checkstyle/pmd）。
 */
public record StaticCheckResult(
        List<CheckstyleViolation> checkstyle,
        List<PmdViolation> pmd,
        String checkstyleJson,
        String pmdJson,
        int checkstyleErrorCount,
        int checkstyleWarningCount) {

    public int totalPmd() {
        return pmd == null ? 0 : pmd.size();
    }
}
