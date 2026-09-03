package com.eduagent.resource.collector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
public class ChunkProcessorTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int MAX_CHARS = 3200; // 约 800 token (1 token ≈ 4 chars)

    @Test
    public void processChunks() {
        // 查询已清洗的语料
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, content FROM kb_corpus WHERE status = 'cleaned'"
        );

        for (Map<String, Object> row : rows) {
            Long corpusId = (Long) row.get("id");
            String content = (String) row.get("content");

            // 按章节分割（假设章节以 "# " 或 "## " 开头）
            List<Chapter> chapters = splitByChapters(content);

            int globalIndex = 0;
            for (Chapter ch : chapters) {
                String chapterTitle = ch.title;
                String chapterContent = ch.content;

                // 按固定窗口切分
                List<String> chunks = splitByWindow(chapterContent, MAX_CHARS);

                for (String chunk : chunks) {
                    jdbcTemplate.update(
                        "INSERT INTO kb_chunks (corpus_id, content, chunk_index, chapter) VALUES (?, ?, ?, ?)",
                        corpusId, chunk, globalIndex++, chapterTitle
                    );
                }
            }
            System.out.println("处理完成: corpusId=" + corpusId + ", 总块数=" + globalIndex);
        }
        System.out.println("分块完成");
    }

    // 按章节标题分割（支持 #, ##, ### 等）
    private List<Chapter> splitByChapters(String content) {
        List<Chapter> chapters = new ArrayList<>();
        // 正则匹配 Markdown 标题行
        Pattern pattern = Pattern.compile("(?m)^(#{1,6})\\s+(.*)$");
        Matcher matcher = pattern.matcher(content);
        int lastStart = 0;
        String lastTitle = "default";
        boolean found = false;
        while (matcher.find()) {
            if (found) {
                // 上一个章节的内容从 lastStart 到当前匹配开始
                String sectionContent = content.substring(lastStart, matcher.start()).trim();
                if (!sectionContent.isEmpty()) {
                    chapters.add(new Chapter(lastTitle, sectionContent));
                }
            }
            lastTitle = matcher.group(2).trim();
            lastStart = matcher.start();
            found = true;
        }
        // 处理最后一个章节
        if (found) {
            String sectionContent = content.substring(lastStart).trim();
            if (!sectionContent.isEmpty()) {
                chapters.add(new Chapter(lastTitle, sectionContent));
            }
        } else {
            // 没有标题，整个作为默认章节
            chapters.add(new Chapter("default", content));
        }
        return chapters;
    }

    // 按固定字符数切分（滑动窗口，不重叠）
    private List<String> splitByWindow(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= maxChars) {
            chunks.add(text);
            return chunks;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            // 尽量在句子边界断开（找最近的句号、换行等，简化处理）
            if (end < text.length()) {
                int lastPunct = Math.max(
                    text.lastIndexOf('.', end),
                    Math.max(text.lastIndexOf('!', end), text.lastIndexOf('?', end))
                );
                if (lastPunct > start) {
                    end = lastPunct + 1;
                }
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            start = end;
        }
        return chunks;
    }

    private static class Chapter {
        String title;
        String content;
        Chapter(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
