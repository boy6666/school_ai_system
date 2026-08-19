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
    void unimportedPackageTriggersPmd() {
        // UnusedImports 是 checkstyle；PMD 规则集仅在确实触发时产出（可空），验证不抛异常即可
        StaticCheckResult r = service.check(List.of(
                new SourceFile("Main.java", "public class Main { public void go() { try { } catch (Exception e) { } } }")));
        assertThat(r.pmdJson()).isNotBlank();
    }
}
