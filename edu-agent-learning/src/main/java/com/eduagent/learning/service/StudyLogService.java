package com.eduagent.learning.service;

import com.eduagent.learning.dto.StudyLogRequest;

import java.util.List;
import java.util.Map;

public interface StudyLogService {

    Map<String, Object> addLog(Long studentId, StudyLogRequest request);

    Map<String, Object> summary(Long studentId);

    Map<String, Object> report(Long studentId);

    List<Map<String, Object>> pendingTasks(Long studentId);

    Map<String, Object> pathSummary(Long studentId);
}
