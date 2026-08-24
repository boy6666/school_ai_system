package com.eduagent.code.service.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 把编译产物（全限定类名→字节码）展开到临时目录，保留包结构。
 * Local 与 Docker 两个 Runner 共用，保证「沙箱只读可见源码目录」逻辑一致。
 */
public final class ClassFileWriter {

    private ClassFileWriter() {
    }

    public static void write(Path root, Map<String, byte[]> classes) throws IOException {
        for (Map.Entry<String, byte[]> e : classes.entrySet()) {
            Path file = root.resolve(e.getKey().replace('.', '/') + ".class");
            Files.createDirectories(file.getParent());
            Files.write(file, e.getValue());
        }
    }
}
