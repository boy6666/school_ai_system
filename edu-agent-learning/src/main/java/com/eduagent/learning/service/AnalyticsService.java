package com.eduagent.learning.service;

import com.eduagent.learning.vo.StudentAnalyticsVO;

import java.util.Map;

/** 学情统计聚合（供 edu-agent-teacher Feign 调用，T/A 角色）。 */
public interface AnalyticsService {

    StudentAnalyticsVO studentAnalytics(Long studentId);

    Map<String, Object> studentProgress(Long studentId);
}
