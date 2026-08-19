package com.eduagent.code.service.compiler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaCompileServiceTest {

    private final JavaCompileService service = new JavaCompileService();

    @Test
    void validSingleFileCompiles() {
        CompileResult r = service.compile(List.of(
                new SourceFile("Main.java", "public class Main { public static void main(String[] a){ System.out.println(\"hi\"); } }")));
        assertThat(r.ok()).isTrue();
        assertThat(r.classes()).containsKey("Main");
    }

    @Test
    void multiFileAndPackageCompile() {
        CompileResult r = service.compile(List.of(
                new SourceFile("com/shop/Dog.java", "package com.shop; public class Dog {}"),
                new SourceFile("com/shop/Main.java",
                        "package com.shop; public class Main { public static void main(String[] a){ new Dog(); System.out.println(\"ok\"); } }")));
        assertThat(r.ok()).isTrue();
        assertThat(r.classes().keySet())
                .contains("com.shop.Main", "com.shop.Dog");
    }

    @Test
    void brokenSourceFailsWithDiagnostics() {
        CompileResult r = service.compile(List.of(
                new SourceFile("Bad.java", "public class Bad { this is not java }")));
        assertThat(r.ok()).isFalse();
        assertThat(r.error()).isNotBlank();
    }
}
