package com.eduagent.code.service.score;

import java.util.Map;

/**
 * 判分结果：总分（0-100）+ 权重明细（供落库 score_detail，对前端不外暴露明细，见 C1）。
 */
public record ScoreResult(int score, Map<String, Integer> detail) {
}
