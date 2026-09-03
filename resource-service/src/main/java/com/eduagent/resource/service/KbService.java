package com.eduagent.resource.service;

import com.eduagent.resource.dto.KbImportReq;
import com.eduagent.resource.dto.KbStatsResp;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KbService {

    private final JdbcTemplate jdbcTemplate;

    // 状态映射
    private String mapStatus(String status) {
        return switch (status) {
            case "0" -> "raw";
            case "1" -> "cleaned";
            case "2" -> "chunked";
            case "3" -> "indexed";
            default -> status;
        };
    }

    // 查询按状态
    public List<Map<String, Object>> getCorpusByStatus(String status) {
        String statusStr = mapStatus(status);
        String sql = "SELECT id, source, LEFT(content, 100) as preview, status, created_at FROM kb_corpus WHERE status = ?";
        return jdbcTemplate.queryForList(sql, statusStr);
    }

    // 标记为已索引
    @Transactional
    public int markIndexed(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toArray(String[]::new));
        String sql = "UPDATE kb_corpus SET status = 'indexed', updated_at = NOW() WHERE id IN (" + placeholders + ")";
        return jdbcTemplate.update(sql, ids.toArray());
    }

    // 导入语料
    @Transactional
    public int importCorpus(KbImportReq req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            return 0;
        }
        int count = 0;
        for (KbImportReq.CorpusItem item : req.getItems()) {
            String sql = "INSERT INTO kb_corpus (content, source, type, status, metadata) VALUES (?, ?, ?, 'raw', ?)";
            jdbcTemplate.update(sql, item.getContent(), item.getSource(), 
                item.getType() != null ? item.getType() : "imported",
                item.getMetadata() != null ? item.getMetadata() : "{}");
            count++;
        }
        return count;
    }

    // 获取统计信息
    public KbStatsResp getStats() {
        // 总语料数
        Long totalCorpus = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kb_corpus", Long.class);
        // 总分块数
        Long totalChunks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kb_chunks", Long.class);
        // 各状态计数
        List<Map<String, Object>> statusRows = jdbcTemplate.queryForList(
            "SELECT status, COUNT(*) as cnt FROM kb_corpus GROUP BY status"
        );
        Map<String, Long> statusCounts = new HashMap<>();
        for (Map<String, Object> row : statusRows) {
            statusCounts.put((String) row.get("status"), (Long) row.get("cnt"));
        }
        KbStatsResp resp = new KbStatsResp();
        resp.setTotalCorpus(totalCorpus);
        resp.setTotalChunks(totalChunks);
        resp.setStatusCounts(statusCounts);
        return resp;
    }
}
