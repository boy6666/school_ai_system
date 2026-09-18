package com.eduagent.learning.service;

import java.util.Map;

public interface DashboardService {

    Map<String, Object> generateAiSummary(Long studentId);

    Map<String, Object> getLatestAiSummary(Long studentId);

    Map<String, Object> generateLearningReview(Long studentId);

    Map<String, Object> getLatestLearningReview(Long studentId);

    Map<String, Object> evaluation(Long studentId);
}
