package com.eduagent.code.service.compiler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内存思路的健壮落地：把多文件写入临时目录（保留包结构）→ {@code javac} 产出字节码 → 用后即清。
 * 跨文件/跨包引用（共识 1：多文件、可多包）天然支持；编译只生成字节码、不执行用户代码，是安全的。
 */
@Slf4j
@Service
public class JavaCompileService {

    /** 匹配源码里的包声明，用于把文件放到正确的包目录 */
    private static final Pattern PKG = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public CompileResult compile(List<SourceFile> sources) {
        if (compiler == null) {
            return CompileResult.fail("当前运行环境没有可用的 javac（仅装了 JRE 未装 JDK）");
        }
        Path root = null;
        Path out = null;
        try {
            root = Files.createTempDirectory("edujava-src-");
            out = Files.createTempDirectory("edujava-cls-");
            List<Path> inputFiles = writeSources(root, sources);

            List<String> options = List.of("-encoding", "UTF-8", "-d", out.toString());
            DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
            try (StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, StandardCharsets.UTF_8)) {
                fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(out.toFile()));
                Iterable<? extends JavaFileObject> units =
                        fm.getJavaFileObjectsFromFiles(inputFiles.stream().map(Path::toFile).toList());
                boolean ok = compiler.getTask(null, fm, diags, options, null, units).call();
                if (!ok) {
                    return CompileResult.fail(formatDiagnostics(diags.getDiagnostics()));
                }
            }
            return CompileResult.success(collectClasses(out));
        } catch (IOException e) {
            log.error("编译临时目录操作失败", e);
            return CompileResult.fail("编译环境异常: " + e.getMessage());
        } finally {
            deleteQuietly(root);
            deleteQuietly(out);
        }
    }

    private List<Path> writeSources(Path root, List<SourceFile> sources) throws IOException {
        List<Path> files = new ArrayList<>();
        for (SourceFile sf : sources) {
            String pkgPath = pkgDir(sf.source());
            Path dir = root;
            if (!pkgPath.isEmpty()) {
                dir = root.resolve(pkgPath);
                Files.createDirectories(dir);
            }
            Path file = dir.resolve(simpleName(sf.name()));
            Files.writeString(file, sf.source(), StandardCharsets.UTF_8);
            files.add(file);
        }
        return files;
    }

    /** 从源码抽包名 → 目录片段（com.shop → com/shop），无包声明返回空串 */
    private String pkgDir(String source) {
        Matcher m = PKG.matcher(source);
        return m.find() ? m.group(1).replace('.', '/') : "";
    }

    /** 取纯文件名，忽略 name 里可能带的包路径 */
    private String simpleName(String name) {
        String n = name.replace('\\', '/');
        int slash = n.lastIndexOf('/');
        return slash >= 0 ? n.substring(slash + 1) : n;
    }

    /** 遍历 classes 目录，收集「全限定类名 → 字节码」（含 $ 内部类） */
    private Map<String, byte[]> collectClasses(Path out) throws IOException {
        Map<String, byte[]> classes = new HashMap<>();
        try (var paths = Files.walk(out)) {
            paths.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
                String rel = out.relativize(p).toString();
                String fqn = rel.replace('\\', '.').replace('/', '.').replaceFirst("\\.class$", "");
                try {
                    classes.put(fqn, Files.readAllBytes(p));
                } catch (IOException e) {
                    log.warn("读取编译产物失败 {}", rel, e);
                }
            });
        }
        return classes;
    }

    private String formatDiagnostics(List<Diagnostic<? extends JavaFileObject>> diags) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : diags) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                sb.append(d.getSource() == null ? "" : d.getSource().getName())
                        .append(':').append(d.getLineNumber())
                        .append(": ").append(d.getMessage(null)).append('\n');
            }
        }
        return sb.toString();
    }

    private void deleteQuietly(Path p) {
        if (p != null) {
            try {
                FileSystemUtils.deleteRecursively(p);
            } catch (IOException e) {
                log.warn("清理临时目录失败 {}", p, e);
            }
        }
    }
}
