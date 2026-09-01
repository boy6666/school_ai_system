package com.eduagent.teacher.vo;

import java.util.List;

/**
 * 单生学情（来自 edu-agent-learning /analytics/student/{id}/progress）。
 * 字段与 learning 契约对齐；缺失时由服务端兜底为 null/0。
 */
public record StudentProgressVO(Long studentId, Integer pathProgress,
                                Integer knowledgeMastery, Integer learningSeconds,
                                Integer lastScore, List<String> weakTopics) {
}
