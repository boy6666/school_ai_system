package com.eduagent.code.service.runner;

import com.eduagent.code.dto.RunRequest;
import com.eduagent.code.dto.RunResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 本机 {@code java} 子进程运行器。开发/单测/无 Docker 环境的默认实现；
 * 线程读 stdout/stderr 防管道死锁，超时 {@code destroyForcibly} 强杀。生产环境应切 {@link DockerSandboxRunner}。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "edu-agent.judge.sandbox", havingValue = "local", matchIfMissing = true)
public class LocalJavaRunner implements SandboxRunner {

    private final String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    @Override
    public RunResult run(RunRequest request) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("edujava-run-");
            ClassFileWriter.write(dir, request.classes());

            ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", dir.toString(), request.className());
            pb.redirectErrorStream(false);
            Process p = pb.start();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            Thread ot = pump(p.getInputStream(), out);
            Thread et = pump(p.getErrorStream(), err);

            long start = System.nanoTime();
            boolean finished = p.waitFor(request.timeoutMs(), TimeUnit.MILLISECONDS);
            long cost = (System.nanoTime() - start) / 1_000_000;

            if (!finished) {
                p.destroyForcibly();
                try {
                    p.waitFor(2, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return RunResult.timedOut(out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8), cost);
            }
            ot.join(1_000);
            et.join(1_000);
            String stdout = out.toString(StandardCharsets.UTF_8);
            String stderr = err.toString(StandardCharsets.UTF_8);
            int exit = p.exitValue();
            return exit == 0
                    ? RunResult.ok(stdout, stderr, cost)
                    : RunResult.crashed(exit, stdout, stderr, cost);
        } catch (Exception e) {
            log.warn("本地运行失败 className={}: {}", request.className(), e.toString());
            return RunResult.error(e.getMessage());
        } finally {
            deleteQuietly(dir);
        }
    }

    private Thread pump(InputStream in, ByteArrayOutputStream sink) {
        Thread t = new Thread(() -> {
            try (in) {
                in.transferTo(sink);
            } catch (IOException ignored) {
                // 进程被杀时流关闭属预期
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    private void deleteQuietly(Path p) {
        if (p != null) {
            try {
                FileSystemUtils.deleteRecursively(p);
            } catch (IOException e) {
                log.warn("清理本地运行临时目录失败 {}", p, e);
            }
        }
    }
}
