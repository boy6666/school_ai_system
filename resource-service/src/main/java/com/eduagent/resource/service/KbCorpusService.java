package com.eduagent.resource.service;

import com.eduagent.resource.dto.KbCorpusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KbCorpusService {

    private final JdbcTemplate jdbcTemplate;

    public List<KbCorpusVO> listByStatus(String status) {
        String sql = "SELECT id, content, source, type, status, metadata, created_at, updated_at FROM kb_corpus WHERE status = ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(KbCorpusVO.class), status);
    }

    @Transactional
    public int markIndexed(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toArray(String[]::new));
        String sql = "UPDATE kb_corpus SET status = 'indexed', updated_at = NOW() WHERE id IN (" + placeholders + ")";
        Object[] params = ids.toArray();
        return jdbcTemplate.update(sql, params);
    }
}
