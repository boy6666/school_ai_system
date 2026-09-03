package com.eduagent.resource.collector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@SpringBootTest
public class KnowledgeCollectorTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void collectJavaNotes() throws IOException {
        String baseDir = "D:/school_ai_system/docs/java_notes/";
        Path dir = Paths.get(baseDir);
        if (!Files.exists(dir)) {
            System.err.println("目录不存在: " + baseDir);
            return;
        }

        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".txt"))
                 .forEach(p -> {
                     try {
                         String content = new String(Files.readAllBytes(p));
                         String source = p.getFileName().toString();
                         // 检查是否已存在（按 source 去重）
                         Integer count = jdbcTemplate.queryForObject(
                             "SELECT COUNT(*) FROM kb_corpus WHERE source = ?",
                             Integer.class, source
                         );
                         if (count > 0) {
                             System.out.println("跳过已存在: " + source);
                             return;
                         }
                         String sql = "INSERT INTO kb_corpus (content, source, type, status, metadata) VALUES (?, ?, ?, ?, ?)";
                         jdbcTemplate.update(sql, content, source, "java_note", "raw", "{}");
                         System.out.println("采集成功: " + source);
                     } catch (Exception e) {
                         System.err.println("处理文件失败: " + p + " - " + e.getMessage());
                     }
                 });
        }
        System.out.println("采集完成");
    }
}
