package com.eduagent.code.service.runner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.eduagent.code.dto.RunRequest;
import com.eduagent.code.dto.RunResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Docker 沙箱运行器（dev-wuyoucheng §2.4.4，★安全红线，每项缺一不可）：
 * 内存限制 / CPU 限制 / {@code --network=none} / {@code --read-only} / 代码目录只读挂载 /
 * 超时 {@code killContainer} 强杀 / {@code withAutoRemove} 即用即焚 / 宿主临时目录用后即清。
 * 仅当 {@code edu-agent.judge.sandbox=docker} 时启用；默认走到 {@link LocalJavaRunner}（开发/单测不依赖 Docker）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "edu-agent.judge.sandbox", havingValue = "docker")
public class DockerSandboxRunner implements SandboxRunner {

    private final DockerClient dockerClient;

    @Value("${edu-agent.judge.docker-image:openjdk:17-slim}")
    private String image = "openjdk:17-slim";

    public DockerSandboxRunner(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public RunResult run(RunRequest request) {
        Path hostDir = null;
        String containerId = null;
        try {
            hostDir = Files.createTempDirectory("edujava-docker-");
            ClassFileWriter.write(hostDir, request.classes());

            CreateContainerResponse c = dockerClient.createContainerCmd(image)
                    .withCmd("sh", "-c", "cd /code && java -cp /code " + request.className())
                    .withHostConfig(HostConfig.newHostConfig()
                            .withMemory(request.maxMemoryMb() * 1024L * 1024L)
                            .withCpuCount(1L)
                            .withNetworkMode("none")
                            .withReadonlyRootfs(true)
                            .withBinds(new Bind(hostDir.toString(), new Volume("/code"), AccessMode.ro))
                            .withAutoRemove(true))
                    .exec();
            containerId = c.getId();

            dockerClient.startContainerCmd(containerId).exec();

            long start = System.nanoTime();
            WaitContainerResultCallback waiter = dockerClient.waitContainerCmd(containerId).start();
            Integer exit = waiter.awaitStatusCode(request.timeoutMs(), TimeUnit.MILLISECONDS);
            long cost = (System.nanoTime() - start) / 1_000_000;

            if (exit == null) {
                dockerClient.killContainerCmd(containerId).exec();
                return RunResult.timedOut("", "", cost);
            }

            String log = readLog(containerId);
            return exit == 0
                    ? RunResult.ok(log, "", cost)
                    : RunResult.crashed(exit, log, "", cost);
        } catch (Exception e) {
            log.warn("Docker 沙箱运行失败 className={}: {}", request.className(), e.toString());
            return RunResult.error(e.getMessage());
        } finally {
            deleteQuietly(hostDir);
        }
    }

    /** 异步收流 stdout+stderr 帧，超时/容器已自动移除时尽量返回已收到内容 */
    private String readLog(String containerId) {
        try {
            StringBuilderAccumulator acc = new StringBuilderAccumulator();
            dockerClient.logContainerCmd(containerId).withStdOut(true).withStdErr(true).exec(acc);
            acc.awaitCompletion(5, TimeUnit.SECONDS);
            return acc.toString();
        } catch (Exception e) {
            log.warn("读取容器日志失败 containerId={}: {}", containerId, e.toString());
            return "";
        }
    }

    /** LogContainerResultCallback 子类：累积 stdout/stderr 帧文本 */
    private static class StringBuilderAccumulator extends LogContainerResultCallback {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public void onNext(Frame item) {
            super.onNext(item);
            if (item != null && item.getPayload() != null) {
                sb.append(new String(item.getPayload(), StandardCharsets.UTF_8));
            }
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    private void deleteQuietly(Path p) {
        if (p != null) {
            try {
                FileSystemUtils.deleteRecursively(p);
            } catch (IOException e) {
                log.warn("清理沙箱宿主临时目录失败 {}", p, e);
            }
        }
    }
}
