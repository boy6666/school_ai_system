package com.eduagent.code.service.checker;

import com.eduagent.code.service.compiler.SourceFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaticCheckServiceTest {

    private final StaticCheckService service = new StaticCheckService(new ObjectMapper());

    @Test
    void cleanCodeHasNoViolations() {
        StaticCheckResult r = service.check(List.of(
                new SourceFile("Main.java", "public class Main { public static void main(String[] args) { }\n}")));
        assertThat(r.checkstyle()).isEmpty();
        assertThat(r.checkstyleJson()).isNotBlank();
    }

    @Test
    void magicNumberTriggersCheckstyleWarning() {
        StaticCheckResult r = service.check(List.of(
                new SourceFile("Main.java", "public class Main { public static void main(String[] args) { int x = 42; } }")));
        assertThat(r.checkstyle()).anyMatch(v -> "MagicNumberCheck".equals(v.rule()));
    }

    @Test
    void emptyCatchBlockTriggersPmd() {
        // 空 catch 触发 EmptyCatchBlock（PMD 7 该规则在 errorprone 类别）——断言真实命中，
        // 防止 PMD 引擎静默降级却仍返回空结果的情况发生
        StaticCheckResult r = service.check(List.of(
                new SourceFile("Main.java", "public class Main { public void go() { try { } catch (Exception e) { } } }")));
        assertThat(r.pmd()).anyMatch(v -> "EmptyCatchBlock".equals(v.rule()));
        assertThat(r.pmdJson()).contains("EmptyCatchBlock");
        assertThat(r.totalPmd()).isGreaterThan(0);
    }
}
